import time as t
import sys

for i in range(2048):
    print(i % 10, end='', flush=True)
    t.sleep(0.05)

print()

for i in range(16):
    print(f'=========== {i:02d} ===========', file=sys.stderr)
