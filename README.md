
# Sigma Scala

This project implements an Akka-based wrapper for **Sigma**.

**Sigma** is an open standard for rules that allow you to describe searches on log data in generic form. 
These rules can be converted and applied to many log management or SIEM systems and can even be used with 
grep on the command line.

The Sigma Akka wrapper provides an Akka-based HTTP(s) server to interact with the Python based Sigma tool.

This Sigma server accepts request to register new *.yaml configurations and rules. It can also be used to
search for a certain backend, configuration and rule.

As an additional feature, this project supports a Sigma importer that generates Sigma rules from multiple
source like MISP or STIX.
