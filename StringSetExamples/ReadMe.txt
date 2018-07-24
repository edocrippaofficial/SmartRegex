Generic usage: java -jar SmartRegex.jar -f *absolute path to the file* -rS *starting regex*

Please note that this program is not generating the regex from scratch, so the starting regex should be something like [a-z]+ in case of alphanumeric regex.
If you are using generic characters along with classes, please include them in your starting regex

Examples:
- Alphanumeric

	Right regex: 	[e-p]{2}[0-3][a-z]{3}
	Starting regex: [i-j]+


- Hexadecimal colors
	
	Right regex: 	\\#([A-F]|[0-9]){6}
	Starting regex: \\#[2-3]+


- Italian plates

	Right regex: 	[A-Z]{2}[0-9]{3}[A-Z]{2}
	Starting regex: [0-1]+

- Emails

	Right regex: 	[a-z0-9]+\\@[a-z]+\\.[a-z]{2,6}
	Starting regex: [0-1]+\\@[0-4]{2}\\.[a-z]*