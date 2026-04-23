import argparse
import json
from pathlib import Path

import joblib
import numpy as np
import pandas as pd
from sklearn.ensemble import RandomForestRegressor
from sklearn.metrics import mean_squared_error, r2_score
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler


ROOT_DIR = Path(__file__).resolve().parents[1]
DATASET_DIR = ROOT_DIR / "dataset"
MODEL_PATH = Path(__file__).resolve().parent / "soh_model.pkl"
SCALER_PATH = Path(__file__).resolve().parent / "scaler.pkl"
METRICS_PATH = Path(__file__).resolve().parent / "training_metrics.json"


def rule_score(cap, ir, cycle, temp):
    cap_score = np.clip(cap, 0, 100)
    ir_score = np.maximum(0, (1 - ir) * 100)
    cycle_score = np.select(
        [cycle < 500, cycle <= 1000, cycle <= 2000],
        [100, 80, 50],
        default=20,
    )
    temp_score = np.select(
        [temp < 15, temp <= 35, temp <= 45],
        [70, 100, 60],
        default=30,
    )
    return np.rint(cap_score * 0.5 + ir_score * 0.25 + cycle_score * 0.2 + temp_score * 0.05)


def normalize_columns(df):
    renamed = df.copy()
    rename_map = {
        "battery_health_%": "soh",
        "SoH_Percent": "soh",
        "capacity_retained_percent": "capacity_retention_rate",
        "average_battery_temperature_C": "avg_temperature",
        "Avg_Temperature_C": "avg_temperature",
        "avg_temp_celsius": "avg_temperature",
        "charging_cycles": "cycle_count",
        "Total_Charging_Cycles": "cycle_count",
        "charge_cycles": "cycle_count",
        "Internal_Resistance_Ohm": "internal_resistance_ohm",
    }
    for source, target in rename_map.items():
        if source in renamed.columns:
            renamed = renamed.rename(columns={source: target})
    return renamed


def derive_features(df):
    frame = normalize_columns(df)
    out = pd.DataFrame()

    if "capacity_retention_rate" in frame.columns:
        out["capacity_retention_rate"] = pd.to_numeric(frame["capacity_retention_rate"], errors="coerce")
    elif "soh" in frame.columns:
        out["capacity_retention_rate"] = pd.to_numeric(frame["soh"], errors="coerce")
    else:
        out["capacity_retention_rate"] = np.nan

    if "internal_resistance_ohm" in frame.columns:
        resistance = pd.to_numeric(frame["internal_resistance_ohm"], errors="coerce")
        baseline = max(float(resistance.median(skipna=True)), 0.02)
        out["internal_resistance_ratio"] = np.clip((resistance / baseline) - 1.0, 0.02, 1.2)
    elif "internal_resistance_ratio" in frame.columns:
        out["internal_resistance_ratio"] = pd.to_numeric(frame["internal_resistance_ratio"], errors="coerce")
    else:
        reference = out["capacity_retention_rate"].fillna(80)
        out["internal_resistance_ratio"] = np.clip((100 - reference) / 140.0, 0.05, 0.8)

    if "cycle_count" in frame.columns:
        out["cycle_count"] = pd.to_numeric(frame["cycle_count"], errors="coerce")
    else:
        out["cycle_count"] = np.nan

    if "avg_temperature" in frame.columns:
        out["avg_temperature"] = pd.to_numeric(frame["avg_temperature"], errors="coerce")
    else:
        out["avg_temperature"] = np.nan

    if "soh" in frame.columns:
        out["soh"] = pd.to_numeric(frame["soh"], errors="coerce")
    else:
        out["soh"] = np.nan

    out = out.dropna(how="all")
    out["capacity_retention_rate"] = out["capacity_retention_rate"].fillna(out["capacity_retention_rate"].median())
    out["cycle_count"] = out["cycle_count"].fillna(out["cycle_count"].median())
    out["avg_temperature"] = out["avg_temperature"].fillna(out["avg_temperature"].median())
    out["internal_resistance_ratio"] = out["internal_resistance_ratio"].fillna(out["internal_resistance_ratio"].median())
    out["capacity_retention_rate"] = out["capacity_retention_rate"].clip(40, 100)
    out["internal_resistance_ratio"] = out["internal_resistance_ratio"].clip(0.02, 1.2)
    out["cycle_count"] = out["cycle_count"].clip(0, 5000)
    out["avg_temperature"] = out["avg_temperature"].clip(-10, 65)
    pseudo_soh = rule_score(
        out["capacity_retention_rate"].to_numpy(),
        out["internal_resistance_ratio"].to_numpy(),
        out["cycle_count"].to_numpy(),
        out["avg_temperature"].to_numpy(),
    )
    out["soh"] = out["soh"].fillna(pd.Series(pseudo_soh, index=out.index))
    out["soh"] = out["soh"].clip(0, 100)
    return out


def load_datasets():
    csv_files = sorted(DATASET_DIR.glob("*.csv"))
    if not csv_files:
        raise FileNotFoundError(f"No CSV datasets found in {DATASET_DIR}")
    frames = []
    used_files = []
    for csv_file in csv_files:
        frame = pd.read_csv(csv_file)
        processed = derive_features(frame)
        if not processed.empty:
            frames.append(processed)
            used_files.append(str(csv_file))
    if not frames:
        raise ValueError("No usable dataset rows were produced from the available CSV files.")
    merged = pd.concat(frames, ignore_index=True)
    return merged, used_files


def augment_dataset(df, min_rows=600):
    if len(df) >= min_rows:
        return df
    needed = min_rows - len(df)
    sampled = df.sample(n=needed, replace=True, random_state=42).reset_index(drop=True)
    rng = np.random.default_rng(42)
    sampled["capacity_retention_rate"] = np.clip(
        sampled["capacity_retention_rate"] + rng.normal(0, 1.8, size=len(sampled)),
        40,
        100,
    )
    sampled["internal_resistance_ratio"] = np.clip(
        sampled["internal_resistance_ratio"] + rng.normal(0, 0.03, size=len(sampled)),
        0.02,
        1.2,
    )
    sampled["cycle_count"] = np.clip(
        sampled["cycle_count"] + rng.normal(0, 80, size=len(sampled)),
        0,
        5000,
    ).round()
    sampled["avg_temperature"] = np.clip(
        sampled["avg_temperature"] + rng.normal(0, 1.5, size=len(sampled)),
        -10,
        65,
    )
    sampled["soh"] = np.clip(
        sampled["soh"] + rng.normal(0, 1.2, size=len(sampled)),
        0,
        100,
    )
    return pd.concat([df, sampled], ignore_index=True)


def train():
    dataset, used_files = load_datasets()
    dataset = augment_dataset(dataset)
    features = dataset[["capacity_retention_rate", "internal_resistance_ratio", "cycle_count", "avg_temperature"]]
    labels = dataset["soh"]

    x_train, x_test, y_train, y_test = train_test_split(
        features, labels, test_size=0.2, random_state=42
    )

    scaler = StandardScaler()
    x_train_scaled = scaler.fit_transform(x_train)
    x_test_scaled = scaler.transform(x_test)

    model = RandomForestRegressor(n_estimators=100, random_state=42)
    model.fit(x_train_scaled, y_train)
    predictions = model.predict(x_test_scaled)

    r2 = r2_score(y_test, predictions)
    rmse = float(np.sqrt(mean_squared_error(y_test, predictions)))

    joblib.dump(model, MODEL_PATH)
    joblib.dump(scaler, SCALER_PATH)

    summary = {
        "dataset_files": used_files,
        "row_count": int(len(dataset)),
        "r2": round(float(r2), 4),
        "rmse": round(rmse, 4),
        "model_path": str(MODEL_PATH),
        "scaler_path": str(SCALER_PATH),
    }
    METRICS_PATH.write_text(json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(summary, ensure_ascii=False))


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.parse_args()
    train()
