import time
import sys
from tqdm import tqdm


for i in tqdm(range(1000), file=sys.stdout):
    time.sleep(0.05)

print()
