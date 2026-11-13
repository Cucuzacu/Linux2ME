# Linux2ME

Run Linux on old J2ME phones!



This is a port of https://github.com/cnlohr/mini-rv32ima to j2me.

It uses only a few kb of ram and 8 mb of storage so it can run on old phones without running out of memory.

Input:

Arrow keys do what arrow keys always did.
The fire button sends the command.
"#" opens a textbox to type, on QWERTY phones just use the keyboard.

## RAM Cache Pages

Increase the amount of cache pages as much as you can until the app crashes because of an Out of Memory Exception.

## Usage

###### If your phone has sd card support:

Put the Image.bin in the root of you sd card, plug the sd card into the phone and install Linux2ME.jar from releases.

###### If your phone doesn't have sd card support:

Install Linux2ME\_big.jar from releases, but I don't guarantee it will work, most phones have a jar size limit. It can also be used in an emulator for testing.



Allow it to read files from the phone. It will stay for about 1 minute loading the image, just wait, it will be relatively fast after that. Or at least, way, way faster than I expected.

# License

MIT

