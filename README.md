# SecretLib
TopSecret library &amp; commandline utility

Encode your secret data files into images & pdf, known as "cover" files.

## What to do with it ?
Store securely...
- **secret files**
- **crypto keys**
- **seed phrases**
- transmit mail-filtered files
- share multi-secret file infos

...into some *innocent* vacation or family photos or any PDF document.
-> It can now be stored in the cloud and kept **secure** !

## Strong encryption and low-noise generation
Strong encryption (AES) along with strong hash algo (SHA-512 by default, customizable)

The image encoding process uses a semi-random positional algo to place the bit stream into the piture's items at low-level noise.
The PDF encoding inserts new keys into internal maps. No visual impact, no embedded files. Even PDF editors do not show these infos.

## Works for JPEG & PNG images and PDF files.
Retrieve your secrets with 2 security factors :
- **Master password** + **Hash algo** (image en/decoding)
- **Data password** (data en/decoding)

## License

[MIT](http://opensource.org/licenses/MIT)
