# MQTTComponents
###### A Light Java Client Daemon for MQTT communication

## Purpose
This small, modular, Apache-licensed application provides a simple framework to subscribe and publish on topics on any Paho-supported MQTT broker.

## Design
In a nutshell, the application works in a modular pattern. Components are loaded from the "components/" directory and will run along side with many other components. Each component, other than the core components (built-in), has it's own update 
