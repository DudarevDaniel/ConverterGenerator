ConverterGenerator 1.0.0
--------------------------

Custom plugin for Intellij Idea for generating converter method by matching setters and getters of given classes.
Plugin generates the converter method (code) for you in your class.

Installation
------------
Using Intellij Idea built-in system:
  - Preferences/Plugins/Browse repositories.../Search for "converter generator"/Install Plugin/Restart IDE.

Usage
------------
1. Put caret in any place within the class, press Alt+Ins and select in menu "Generate converter method" or use shortcut Ctrl+Alt+G.
2. In the dialog select the Class you want to convert To and select the class you want to convert From.
3. Press "Ok" and converter method will be added to your current class.
4. Plugin also writes in comments list of fields, that were not mapped (appropriate setter or getter is missing or different types).

Example of the result:

     public Dto convertAs(Entity from) {
         Dto to = new Dto();
         to.setName(from.getName());
         to.setAge(from.getAge());
         to.setAddress(from.getAddress());
         to.setNeighbors(from.getNeighbors());
         to.setStudent(from.getStudent());

         // Not mapped FROM fields:
         // id
         // preferredLanguage
         return to;
     } 

