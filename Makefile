CC = kotlinc
RM = del
SRC = src/main/kotlin/zx5/

all: zx5 

zx5: $(SRC)Main.kt $(SRC)Block.kt $(SRC)Cell.kt $(SRC)Offsets.kt $(SRC)Optimizer.kt $(SRC)Compressor.kt
	$(CC) $(SRC)Main.kt $(SRC)Block.kt $(SRC)Cell.kt $(SRC)Offsets.kt $(SRC)Optimizer.kt $(SRC)Compressor.kt -include-runtime -d zx5.jar

clean:
	$(RM) zx5.jar
