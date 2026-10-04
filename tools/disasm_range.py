#!/usr/bin/env python3
"""Bounded address range from the already generated original EXE disassembly."""
import re,sys
from pathlib import Path
lo,hi=(int(a,16) for a in sys.argv[1:3])
for line in (Path(__file__).resolve().parents[1]/'research/runtime_disassembly.txt').read_text().splitlines():
    m=re.match(r'\s*([0-9a-f]+):',line)
    if m and lo<=int(m[1],16)<hi:print(line)
