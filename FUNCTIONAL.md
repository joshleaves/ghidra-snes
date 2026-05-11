# Functional tests

The [functional test suite](src/test/java/ghidra_snes/FunctionalTestRealDataTest.java) works on real data and its data resources are not meant to be committed, only to be tested locally by extension developers. It'll be an exercise to the reader to populate the `src/test/resources/data` with real data, as identified by their SHA256.

| Data name | Rationale | Status |
|-----------|-----------|--------|
| A plumber all over the world                                  | Base LoROM               | ✅ |
| Women don't get enough representation in Sci-Fi               | Biggest LoROM            | ✅ | 
| A robot from the future suffers a copy protection error       | Notorious mapping issues | ✅ |
| That same robot, but now he's rocking in Japan                | Notorious mapping issues | ✅ |
| That same robot comes back, IN AMERICA                        | CX-4 Chip                | ✅ |
| Japanese, pink, and shaped like a friend                      | SA-1 Chip                | ✅ |
| A plumber tries its hands at a beloved Japanese genre         | SA-1 Chip                | ✅ |
| French attempt to remake the Earth in our image               | Basic HiROM              | ✅ |
| Holidays in Jipang (Episode Zero)                             | SPC7110 Chip             | ✅ |
| Big monsters, Electric Boogaloo                               | Basic ExHiROM            | ✅ |
| These stories are fantastic                                   | The Dhaos of real data   | ✅ |
| A sky to swim for                                             | The last boss of SDD-1   | ⚠️[^err_sdd1] |

[^err_sdd1]: Partial support: the canonical `$C0-$FF` FILE view is mapped, but S-DD1 banks don't map regularly. The loader currently exposes a static HiROM-like view, which can diverge from [Mesen2](https://github.com/SourMesen/Mesen2)'s runtime memory view, especially around `$40+`.


# Notes to add more tests

1. Get the data's SHA256
2. Calculate bank count (file size / 0x8000 or 0x10000)
3. Calculate the SHA256 of boundary banks (ie: first and last)
4. Counter-check the data's first and last 64 bytes against the Mesen2 memory viewer

## Some CLI stuff:

### Calculate a bank's SHA256

```shell
$ dd if=input_lorom bs=1 skip=(math "0x8000 * 0x3f") count=(math 0x8000) 2>/dev/null | shasum -a 256
$ dd if=input_hirom bs=1 skip=(math "0x10000 * 0x3f") count=(math 0x10000) 2>/dev/null | shasum -a 256
```

### Check a file's checksum
(ATTENTION: You'll get the results as LE, you need to invert the bytes to get the BE value)

Nice fish function:
```bash
function snesheaders
  set file $argv[1]

  for pair in "LoROM 0x7FC0" "HiROM 0xFFC0" "ExHiROM 0x40FFC0"
    set name (string split " " $pair)[1]
    set offset (string split " " $pair)[2]

    echo ""
    echo "=== $name header @ $offset ==="

    dd if=$file bs=1 skip=(math $offset) count=64 2>/dev/null | xxd

    set checksum_offset (math "$offset + 0x1E")
    set checksum (dd if=$file bs=1 skip=$checksum_offset count=2 2>/dev/null | xxd -p -c 2)

    echo "Checksum bytes: $checksum"
  end
end
```

### Check a bank's contents
Useful to compare against an emulator's memory view.

```bash
$ echo "=== FIRST 0x40 BYTES ==="
$ dd if=$file \
  bs=1 skip=(math "$skip * $size") count=64 2>/dev/null \
  | xxd
$ echo ""
$ echo "=== LAST 0x40 BYTES ==="
$ dd if=$file \
  bs=1 skip=(math "$skip * $size + $size - 0x40") count=64 2>/dev/null \
  | xxd
```

Or as a cool [fish-shell](https://github.com/fish-shell/fish-shell) function:
```bash
function snesbankxxd
  set file $argv[1]
  set skip $argv[2]
  set size $argv[3]

  echo "=== FIRST 0x40 BYTES ==="
  dd if=$file \
    bs=1 skip=(math "$skip * $size") count=64 2>/dev/null \
    | xxd
  echo ""
  echo "=== LAST 0x40 BYTES ==="
  dd if=$file \
    bs=1 skip=(math "$skip * $size + $size - 0x40") count=64 2>/dev/null \
    | xxd
end
```


