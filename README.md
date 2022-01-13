Compilable programming language written in Java.

In the beginning syntatic analysis stage was written completely from scratch. As I was implementing more and more features in the compiler, it started getting more compilated, thus I rewrote syntatic part which is now using ANTLR for source code parsing. While it does syntactic analysis at the same time abstract syntax tree is being constructed.

After syntactic analysis is done and abstract syntax tree (AST) is constructed, it's ready for semantic analysis. I'm not an expert in computer science, just a self taught programmer, so it may not be the best semantic analysis, but I did as best as I can with my knowledge and with the help of an internet.

If semantic analysis didn't throw any error, compiler starts building assembly code. As every other part of the compiler, this part was also written completely from scratch by myself. I had a little bit of experience with assembly language previously (x86 Intel syntax). I did read a lot about stack / heap memory, learn about expression solving and etc while creating assembly code builder, but it wasn't that hard to create it. The hardest part was (and still is) to build assembly code as efficient as possible. I cannot say that it's building very efficient assembly code right now, but I always keep it in mind when working with assembly code builder.

In order to get an executable, nasm is being called to compile assembly code into ELF file and then ELF files are linked with system libraries to get an executable.

Compiler do not have any optimization part yet. I have considered about it previously, but I won't rush into it until I don't finish basic compiler features completely.

About language itself

Syntax is very similar to c/c++. Primitive data types: bool, char, short, int, string, struct. Struct can have their own functions like classes have in OOP. At the moment pointers & references are supported in the language, but I have considered about removing that feature several times. Pointers sometimes helps to debug the code, so it's actually useful sometimes when debugging compiler, but it makes language look harder to understand. Compiler also have few built-in functions: __asm__, len, sizeof. printf is built-in function, but what compiler actually does when printf is called, is just overwriting builtin printf call with call to printf function inside system library, and yes, any function from system library can be called. I've done it before, but since I added support for packages, calling function from system library is currently not possible, but it can be implemented.
