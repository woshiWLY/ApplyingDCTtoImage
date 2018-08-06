# ApplyingDCTtoImage
using dct to compress image and translate it

### compile as 
```sh
javac imageReader.java
```

### Run as 
```sh
java image.rgb quantizeLevel mode latency
```

#### parameters
```sh
InputImage quantizationLevel DeliveryMode Latency
```
    - InputImage
      is the image to input to your coder-decoder 
    - quantizationLevel
      a factor that will decrease/increase compression as explained below. This value will range from 0 to 7
    - DeliveryMode
      an index ranging from 1, 2, 3. 
      A 1 implies baseline delivery
      a 2 implies progressive delivery using spectral selection
      a 3 implies progressive delivery using successive bit approximation.
    - Latency
      a variable in milliseconds, which will give a suggestive “sleep” time between data blocks during decoding. 
      This parameter will be used to “simulate” low and high band width decoding to properly evaluate the simulation of your delivery modes.
