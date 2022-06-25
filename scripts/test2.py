import time
import sys

for s in ['this', 'is', 'one', 'string']:
    print(s, end=' ')
    time.sleep(1.5)

print()

for i in range(1000):
    pr = 22 * i // 1000
    print("\r", end='')
    print("*" * pr, end='')
    time.sleep(0.05)

print()
