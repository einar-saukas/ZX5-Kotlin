# ZX5-Kotlin

**ZX5-Kotlin** is a multi-thread implementation of the
[ZX5](https://github.com/einar-saukas/ZX5) data compressor in
[Kotlin](https://kotlinlang.org/).


## Requirements

To run this compressor, you must have installed [Java](https://www.java.com/) 8 
or later.


## Usage

To compress a file such as "Cobra.scr", use the command-line compressor as
follows:

```
java -jar zx5.jar Cobra.scr
```

Java 8 memory allocation is limited to (at most) 1Gb by default. You can use 
parameter "-Xmx" to increase maximum memory allocation, for instance:

```
java -Xmx2G -jar zx5.jar Cobra.scr
```

This compressor uses 16 threads by default. You can use parameter "-p" to
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

* [ZX0-Kotlin](https://github.com/einar-saukas/ZX0-Kotlin) - A similar
multi-thread data compressor for [ZX0](https://github.com/einar-saukas/ZX0),
by the same author.
