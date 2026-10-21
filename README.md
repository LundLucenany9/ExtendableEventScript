# ExtendableEventScript (EES)

ExtendableEventScript (EES) is an extensible scripting language implemented in Java. It provides its own lexer, parser, compiler, bytecode format, and virtual machine, allowing scripts to be compiled and executed by Java applications.

EES is designed to be extended through Java libraries, making it possible to expose application-specific functionality to scripts.

## Features

- Indentation-based syntax
- Variables, functions, and return values
- Conditional statements with `if`, `elif`, and `else`
- Counted loops using `for rovar`
- Foreach loops using `foreach rovar rovar`
- Lists and maps
- String interpolation using `$`
- Optional named arguments
- Java library integration
- Custom bytecode compilation and execution
- Optional compression of compiled scripts
- Graphical debugging tools

## Example

The following example demonstrates the syntax and several language features.

```python
import std
import std.io

log "Hello World"

for rovar i -> 0-10
    log $"i is $($i + 1)"

log "Reverse!!"

for rovar i -> 5-0
    log $i

log "i is " {newline = false}
log $i

if 5 == 6
    log "5 is 6"
elif 6 == 3
    log "6 is not 3"
else
    log "5 is not 6"

if 5 == 5
    log "if"

log $"i is $i"

log (["a", "b", "c"] + "d")[3]
log ({"a"=1, "b"=2,"c"=3}["c"])

function add(a,b)
    log $"a is $a, b is $b"
    return $a + $b

add 1 2
log add 5 100

log #i["defined"]

rovar x = 1

log ({"a"={"b"="hi","c"=[add 1 2,"b"]}})["a"]["c"][0]{newline=false}

logf "lex %.2f ms, parse %.2f\n" ([1.0, 2.0])

log $"hi\nlol"

writeln $"Hello, $name!"
var iterable = {"a"="b", "c"="C"}
foreach rovar i, rovar value -> $iterable
    log $"$i: $value"
```

## Syntax Overview

### Imports

Import libraries by name:

```python
import std
import std.io
```

Only registered libraries can be imported by the script.

### Variables

Declare a variable with `var` or a read-only variable with `rovar`, according to the language's variable rules.

```python
var x = 1
rovar message = "Hello"
```

### Loops

Counted loops use the `for rovar` syntax and an arrow followed by a range:

```python
for rovar i -> 0-10
    log $i

for rovar i -> 5-0
    log $i
```

Foreach loops use the `foreach rovar rovar` or `foreach rovar` syntax and an arrow followed by an iterable such as a list or a map
```python
foreach rovar i, rovar value -> $iterable
    log $"$i: $value"
    
foreach rovar value -> $iterable
    log $value
```

### Functions

Declare functions with `function`. Parameters are written in parentheses, while function calls can use positional arguments without parentheses.

```python
function add (a,b)
    return $a + $b

log add 5 100
```

Functions declared with optional arguments are declared as such
```python
function add (a,b) {c,d}
    var x = $a + $b
    if #c["defined"]
        x = x + c
    if #d["defined"]
        x = x + d
    return x
log add 5 10
log add 5 10 {c=15}
log add 5 10 {d=20}
log add 5 10 {c=15,d=20}
```

### Conditional statements

EES supports `if`, `elif`, and `else`:

```python
if 5 == 6
    log "5 is 6"
elif 6 == 3
    log "6 is not 3"
else
    log "5 is not 6"
```

### String interpolation

Prefix strings with `$` to interpolate variables and expressions:

```python
log $"i is $i"
log $"i is $($i + 1)"
```

### Lists and maps

Lists and maps can be created directly in expressions and accessed by index or key:

```python
log (["a", "b", "c"] + "d")[3]
log ({"a"=1, "b"=2, "c"=3}["c"])
```

Nested collections and expressions are also supported.

### Optional named arguments

Optional named arguments are supplied in a trailing options block:

```python
log "i is " {newline = false}
```

The option names and defaults are defined by the Java methods exposed to EES or by a function definition

## Command-Line Usage

Run the application using Java:

```bash
java -jar EEScript.jar <option> [arguments]
```

### Compile and compress

```bash
java -jar EEScript.jar --compile script.ees
```

Compiles the source script and compresses the resulting bytecode. The default output filename is `script.eex`.

Choose an output file explicitly:

```bash
java -jar EEScript.jar --compile script.ees --output program.eex
```

### Compile without compression

```bash
java -jar EEScript.jar --compile-only script.ees
```

Compiles the source script without compression. The default output filename is `script.eeb`.

### Execute a compiled script

```bash
java -jar EEScript.jar --execute program.eeb
```

### Execute with debugging

```bash
java -jar EEScript.jar --debug program.eeb
```

The debug mode opens a graphical interface for inspecting disassembled code, runtime output, and errors.

### Display help

```bash
java -jar EEScript.jar --help
```

| Option | Description |
|---|---|
| `-c`, `--compile` | Compile and compress a script |
| `-C`, `--compile-only` | Compile without compression |
| `-e`, `--execute` | Execute a compiled script |
| `-d`, `--debug` | Execute a compiled script with debugging |
| `-o`, `--output` | Specify the output file for compilation |
| `-h`, `--help` | Display help |

## Embedding EES in Java

EES can be embedded directly into a Java application. A typical workflow is to tokenize the source, parse it, compile it, and execute the resulting bytecode with a configured host.

```java
SimpleHost host = SimpleHost.getStandardHost();

String source = """
        import std

        log "Hello from EES!"
        """;

List<Token> tokens = new Lexer(source).tokenize();
Program program = new Parser(tokens).parse();
byte[] bytecode = new Compiler().compile(program);
// or
byte[] bytecode = new Compiler().compile(source);

VM vm = new VM(host);
vm.run(bytecode, new HashMap<>(), new HashMap<>());
```

This example uses the standard libraries provided by `SimpleHost.getStandardHost()`. The maps passed to `vm.run()` initialize the execution environments.

## Registering Custom Libraries

Java applications can extend EES by registering custom libraries through `SimpleHost.Builder`. Methods annotated with `@EESFunction` are exposed to scripts.

### 1. Implement a library

Implement the `Library` interface and return a unique name:

```java
public class MyLibrary implements Library {

    @Override
    public String getName() {
        return "my";
    }

    @EESFunction
    public void greet(String name) {
        System.out.println("Hello, " + name + "!");
    }
}
```

The library name is used in the script's import statement. Unless a custom function name is specified in `@EESFunction`, the Java method name is used.

### 2. Register the library

```java
SimpleHost host = new SimpleHost.Builder()
        .addDefaultLibraries()
        .registerLibrary(new MyLibrary())
        .build();
```

The builder supports method chaining. To register only custom libraries, omit `addDefaultLibraries()`.

Library names must be unique within a host. Attempting to register a duplicate name results in a `LibraryRegistrationException`.

### 3. Import and call the library

```python
import my

greet "EES"
```

### 4. Execute a script using the custom host

```java
SimpleHost host = new SimpleHost.Builder()
        .addDefaultLibraries()
        .registerLibrary(new MyLibrary())
        .build();

String source = """
        import my

        greet "EES"
        """;

byte[] bytecode = new Compiler().compile(source);

VM vm = new VM(host);
vm.run(bytecode, new HashMap<>(), new HashMap<>());
```

The host determines which libraries are available. Scripts must import a library before calling its functions.

## Optional Named Arguments in Java Libraries

Use `@EESOptional` to expose an optional named argument. The annotation's `name` value defines the name used in EES source code. The Java method receives an `Optional<T>` for the optional parameter.

For example:

```java
@EESFunction
public void log(
        Object message,
        @EESOptional(name = "newline") Optional<Boolean> newline
) {
    boolean nl = newline.orElse(true);
    if (nl) message += "\n";
    System.out.print(message);
}
```

The `newline` argument defaults to `true` if it is omitted.

### Calling the method from EES

```python
import std

log "Hello"
log "Hello" {newline = false}
log "Hello" {newline = true}
```

- `log "Hello"` prints a newline by default.
- `log "Hello" {newline = false}` suppresses the trailing newline.
- `log "Hello" {newline = true}` explicitly enables it.

Optional named arguments are specified in a trailing options block using the syntax `{name = value}`. The option name must match the name declared by `@EESOptional`.

## Project Status

ExtendableEventScript is under active development. Language syntax, bytecode formats, and public APIs may change as the project evolves.

## License

See the repository's license file for licensing terms.
