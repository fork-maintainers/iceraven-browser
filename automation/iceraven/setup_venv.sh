#!/usr/bin/env bash

python -m venv venv
source venv/bin/activate
python -m pip install --trusted-host pypi.python.org --no-cache-dir glean_parser~=20.0