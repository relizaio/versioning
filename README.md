# Reliza Versioning

This tool allows for automatic generation and bumping of [CalVer](https://calver.org/) or [SemVer](https://semver.org/) or custom version schemas. Motivational article: https://worklifenotes.com/2020/02/27/automatic-version-increments-with-reliza-hub-2-strategies/

If you are looking for a versioning server using this library for automated software version management, refer to [Project ReARM](https://github.com/relizaio/rearm).


<p align="center"><img src="/img/terminal_demo.gif?raw=true"/></p>


## 1. Features
- Generate CalVer, SemVer, or Four-Part versions
- Bump CalVer, SemVer, or Four-Part versions
- Flexibility in creating and managing different flavors of versioning schemas
- Usage as either Java Library or a CLI tool

## 2. Sample Command Line usage

#### 2.1. Generate Reliza flavor CalVer with "Stable" modifier
```
java -jar versioning.jar -s YYYY.0M.Calvermodifier.Micro+Metadata -i Stable
```
or with docker:
```
docker run --rm relizaio/versioning -s YYYY.0M.Calvermodifier.Micro+Metadata -i Stable
```

#### 2.2. Bump patch in existing SemVer version (will produce 2.4.8)
```
java -jar versioning.jar -s semver -v 2.4.7 -a Bump
```
or with docker:
```
docker run --rm relizaio/versioning -s semver -v 2.4.7 -a Bump
```

#### 2.2.1. Bump nano in existing Four-Part version (will produce 1.2.3.5)
```
java -jar versioning.jar -s four_part -v 1.2.3.4 -a Bump
```
or with docker:
```
docker run --rm relizaio/versioning -s four_part -v 1.2.3.4 -a Bump
```

#### 2.3. Sample call inside Reliza Versioning itself to bump version in the project's pom file, with snapshot option (-t flag) set to true:
```
gradle changeVersion -PnewVersion="$(java -jar path_to_versioning\versioning.jar -s yyyy.0m.Calvermodifier.patch -i Stable -t True)"
```
or with docker:
```
gradle changeVersion -PnewVersion="$(docker run --rm relizaio/versioning -s yyyy.0m.Calvermodifier.patch -i Stable -t True)"
```
Note that this example is using versions-maven-plugin (that Reliza Versioning is using too).

Similarly, Reliza Versioning can be included into Jenkins by being called from the bash scripts.

#### 2.4. Show help page

```
docker run --rm relizaio/versioning -h
```
which will produce help summary page.

Note that in any usage case other than help page -s (schema) parameter is required.

#### 2.5. Known version elements:

Reliza Versioning understands following elements of versioning schema (case insensitive):
- **Major**
- **Minor**
- **Micro** (or Patch, Build, Bugfix)
- **Nano** (or Revision, Hotfix) - used in Four-Part versioning
- **Year** (or YYYY) - 4-digit year presentation
- **YY** - 2-digit year presentation, if 1st digit is 0, only second digit is shown
- **OY** - 2 digit year presentation, if 1st digit is 0, it's still displayed as 0
- **YYOM** - 2-digit year presentation with month, i.e. 2103 is March, 2021
- **YYYYOM** - 4-digit year presentation with month, i.e. 202103 is March, 2021
- **MM** - 2-digit month presentation, if 1st digit is 0, only second digit is shown
- **OM** - 2-digit month presentation, if 1st digit is 0, it's still displayed as 0
- **DD** - 2-digit day presentation, if 1st digit is 0, only second digit is shown
- **OD** - 2-digit day presentation, if 1st digit is 0, it's still displayed as 0
- **CIENV**
- **CIBUILD**
- **BRANCH** - accepts any keyword, suggested for feature branch names
- **MODIFIER** - semver modifier (see note below), by convention separated by minus (-), if another separator is not specified
- **CALVERMODIFIER** - calver modifier (not to be used with SemVer)
- **METADATA** - can be used with both CalVer or SemVer, by convention separated by plus (+), if another separator is not specified

Dot (.), underscore(_) may be used as separators. Dash (-) or plus (+) may be used as separators once each specifically for modifier and metadata. We recommend using dash for modifier and plus for metadata as per SemVer conventions, which then would be treated by the tool as optional elements.

Note: for SemVer always use "modifier" notation, for CalVer still use "modifier" if it's used after -, i.e. YYYY.MM-modifier, but use calvermodifier in the dot notation, i.e. YYYY.MM.Calvermodifier.Patch

Reliza Versioning also understands the following schema aliases:
- **"SemVer"** - resolves to "Major.Minor.Patch-Modifier?+Metadata?" (where modifier and metadata are optional)
- **"four_part"** - resolves to "Major.Minor.Patch.Nano-Modifier?+Metadata?" for Four-Part versioning (e.g., 1.2.3.4)

## 3 Different ways to use - CLI vs Java Library

### 3.1. For command-line usage or integration with CI/CD tools (such as Jenkins)

#### 3.1.1. Use docker image:
```
docker pull relizaio/versioning
```

#### 3.1.2. Compile locally (requires Java 8+ and maven) as jar CLI tool
from the project directory run
```
gradle build
```

### 3.2. II To use as a java library
#### 3.2.1. Use as maven dependency from maven central:
For Maven:
```
<dependency>
  <groupId>io.reliza</groupId>
  <artifactId>versioning</artifactId>
  <version>2020.11.Stable.1</version>
</dependency>
```

For Gradle:
```
implementation 'io.reliza:versioning:2020.11.Stable.1'
```

See more options on the [Maven Central page](https://search.maven.org/artifact/io.reliza/versioning/)

#### 3.2.2. Compile locally (requires Java 8+ and Gradle)
from the project directory run
```
gradle build
```

Using Gradle: publish to maven local repository:
```
gradle publisToMavenLocal
```

And include resulting .jar file from the target directory in your project. Then use io.reliza.versioning.VersionApi class for most common operations. More documentation coming soon.

## Running the tests (requires Java 8+ and maven)
From the project directory run

Using gralde:
```
gradle test
```

Run a single test:
```
gradle test --tests '*bumpCalverVersionWithPin3*' --info
```

## 4. Usage as Java Library
Use methods exposed in the VersionApi class to create vresions. More documentation is coming soon.

## Authors

This project is created and open-sourced by [Reliza](https://reliza.io)

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details

## Acknowledgments

* This project currently uses Java, Maven, Apache Commons and JUnit.
