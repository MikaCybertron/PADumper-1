# PAD (Process Android Dumper)
This dumper is made for il2cpp game but you can use it in any app you want

## How To Use
- Run the process
- Open PADumper
- Put process name manually or you can click "Select Apps"
- Put the ELF Name (default="libil2cpp.so")
- Check "global-metadata.dat" if you want dump unity metadata from memory
- Dump and wait process to finish
- Result will be in "/sdcard/Download/<'package name'>/<'startAddress-fileName'>"

## Credits
- [libsu](https://github.com/topjohnwu/libsu)