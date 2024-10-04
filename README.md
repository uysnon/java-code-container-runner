
{
"taskId": "12345",
"javaVersion": "11",
"mainClass": "com.example.Main",
"files": [
{
"name": "com/example/Main.java",
"content": "package com.example;\n\npublic class Main {\n    public static void main(String[] args) {\n        System.out.println(\"Привет, мир!\");\n        Helper.sayHello();\n    }\n}\n"
},
{
"name": "com/example/Helper.java",
"content": "package com.example;\n\npublic class Helper {\n    public static void sayHello() {\n        System.out.println(\"Привет от Helper!\");\n    }\n}\n"
}
],
"arguments": ["пример", "аргумента"],
"stdin": ""
}