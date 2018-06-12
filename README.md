# SmartRegex
A tool to find the regex that match more a set of given (or generated) strings using evolutionary computing

## Syntax

```
usage: SmartRegex.jar [-disablehom] [-f <file>] [-h] [-homperc <float [0,1]>]
       [-hyper] [-mono] [-multi] [-multi2] [-ngen <int>] [-ninf <int>]
       [-nospecialize] [-npar <int>] [-npop <int>] [-nstrings <int>] [-rO
       <regex>] [-rS <regex>] [-rU <regex>]
```

Use `SmartRegex -h` to see all the options

The file passed with the set of strings must have this syntax for each line:
```
one string
space
the capital letter C or W (meaning correct or wrong)
```
See stringSet_example.txt 
