import sys,  getopt
from scipy import misc
import matplotlib.pyplot as plt
import numpy as np
inputfile ='lena.raw'
		

lraw = np.fromfile(inputfile, dtype=np.uint8)
lraw.shape = (512,512)
plt.imshow(lraw,cmap=plt.cm.gray)
plt.show()

lraw = np.fromfile('lenablurred.raw', dtype=np.uint8)
lraw.shape = (512,512)
plt.imshow(lraw,cmap=plt.cm.gray)
plt.show()


