# cruci-bot for thecrucible

## What's this? 
Crucibot's purpose is to play KeyForge in the platform [thecrucible.online](https://thecrucible.online/) using AI techniques.
It is fully written in Java and implements some AI tecniques such as:
- Symbolic approach
- Tree Search
- Hybrid tree search with Symbolic approach

## May I contribute?
Scope of work was focused on the algorithm, so several improvements can be done such as the configuration and the user interface.
At the moment the bot will only run if the interface is hard-coded and the site is hosted using the repository [keyteki](https://github.com/keyteki/keyteki).

Also the card KB (knowledge base) is set using a manual file called `cards`, each deck set must be specified in the `deck` files with the UUID contained in keyteki's DB and each deck composition is written inside its own file. This interface should be dynamic and not depending on manual work.

Feel free to improve this code, raise issues and contact me for any question.<br>
Thomas -> thomas.villano2@gmail.com
