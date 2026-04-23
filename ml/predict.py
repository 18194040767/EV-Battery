import argparse
import json
from pathlib import Path

import joblib
import numpy as np
import pandas as pd


BASE_DIR = Path(__file__).resolve().parent
MODEL_PATH = BASE_DIR / "soh_model.pkl"
SCALER_PATH = BASE_DIR / "scaler.pkl"


def load_assets():
    if not MODEL_PATH.exists() or not SCALER_PATH.exists():
        raise FileNotFoundError("Model files not found. Please run ml/train.py first.")
    model = joblib.load(MODEL_PATH)
    scaler = joblib.load(SCALER_PATH)
    return model, scaler


def predict(cap, ir, cycle, temp):
    model, scaler = load_assets()
    features = pd.DataFrame(
        [[cap, ir, cycle, temp]],
        columns=["capacity_retention_rate", "internal_resistance_ratio", "cycle_count", "avg_temperature"],
    )
    scaled = scaler.transform(features)
    predicted = model.predict(scaled)[0]
    return int(round(float(np.clip(predicted, 0, 100))))


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--cap", required=True, type=float)
    parser.add_argument("--ir", required=True, type=float)
    parser.add_argument("--cycle", required=True, type=float)
    parser.add_argument("--temp", required=True, type=float)
    args = parser.parse_args()
    result = {"predicted_soh": predict(args.cap, args.ir, args.cycle, args.temp)}
    print(json.dumps(result, ensure_ascii=False))
