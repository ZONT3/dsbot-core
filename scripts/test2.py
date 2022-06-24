from tqdm import tqdm
import time
import sys

for i in range(1000):
    pr = 22 * i // 1000
    print("\r", end='')
    print("*" * pr, end='')
    time.sleep(0.05)

print()
