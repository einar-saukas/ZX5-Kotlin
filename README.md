# ZX5-Kotlin

**ZX5-Kotlin** is a multi-thread implementation of the
[ZX5](https://github.com/einar-saukas/ZX5) data compressor in
[Kotlin](https://kotlinlang.org/).


## Requirements

This compressor requires [Java](https://www.java.com/) 8 or later.


## Usage

To compress a file such as "Cobra.scr", use the command-line compressor as
follows:

```
java -jar zx5.jar Cobra.scr
```

This compressor executes 16 threads by default. You can use parameter "-p" to 
specify a different number of threads, for instance:

```
java -jar zx5.jar -p4 Cobra.scr
```

All other parameters work exactly like the original version. Check the official
[ZX5](https://github.com/einar-saukas/ZX5) page for further details.


## License

The Kotlin implementation of [ZX5](https://github.com/einar-saukas/ZX5) was
authored by **Einar Saukas** and it's available under the "BSD-3" license.


## Links

* [ZX5](https://github.com/einar-saukas/ZX5) - The original version of **ZX5**,
by the same author.
