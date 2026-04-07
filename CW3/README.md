# CW3

This directory contains:

- the main CW3 events app submission
- the separate Task 4 faculty preregistration submission

## How to run the main application

The main system is a text-based application. Run:

`mvn -q exec:java -Dexec.mainClass=uk.ac.ed.inf.eventsapp.Main`

In the CLI:

- enter menu numbers to choose actions
- enter `0` in a menu to exit
- enter `:q`, `:quit`, or `:exit` at any input prompt to exit immediately

## Preregistered users

The main system loads preregistered student and admin accounts from
`src/main/resources`.

- username/email: `student1@ed.ac.uk`
  password: `student1`
- username/email: `student2@ed.ac.uk`
  password: `student2`
- username/email: `admin1@ed.ac.uk`
  password: `admin1`
