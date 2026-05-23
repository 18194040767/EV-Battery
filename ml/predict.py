import argparse
import json
import sys
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


def predict_with_assets(model, scaler, cap, ir, cycle, temp):
    features = pd.DataFrame(
        [[cap, ir, cycle, temp]],
        columns=["capacity_retention_rate", "internal_resistance_ratio", "cycle_count", "avg_temperature"],
    )
    scaled = scaler.transform(features)
    predicted = model.predict(scaled)[0]
    return int(round(float(np.clip(predicted, 0, 100))))


def predict(cap, ir, cycle, temp):
    model, scaler = load_assets()
    return predict_with_assets(model, scaler, cap, ir, cycle, temp)


def serve():
    model, scaler = load_assets()
    print(json.dumps({"status": "ready"}, ensure_ascii=False), flush=True)
    for line in sys.stdin:
        line = line.strip()
        if not line:
            continue
        try:
            payload = json.loads(line)
            result = predict_with_assets(
                model,
                scaler,
                float(payload["cap"]),
                float(payload["ir"]),
                float(payload["cycle"]),
                float(payload["temp"]),
            )
            print(json.dumps({"predicted_soh": result}, ensure_ascii=False), flush=True)
        except Exception as exc:
            print(json.dumps({"error": str(exc)}, ensure_ascii=False), flush=True)


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--serve", action="store_true")
    parser.add_argument("--cap", type=float)
    parser.add_argument("--ir", type=float)
    parser.add_argument("--cycle", type=float)
    parser.add_argument("--temp", type=float)
    args = parser.parse_args()
    if args.serve:
        serve()
        raise SystemExit(0)
    if args.cap is None or args.ir is None or args.cycle is None or args.temp is None:
        parser.error("--cap, --ir, --cycle and --temp are required unless --serve is used")
    result = {"predicted_soh": predict(args.cap, args.ir, args.cycle, args.temp)}
    print(json.dumps(result, ensure_ascii=False))
