from bluedot import BlueDot
import random

# define device
bd = BlueDot()

for i in range(10):
    bd.wait_for_press
    greenVal = random.randint(0,9)
    bd.color = (0,greenVal,0)
