# Implementations List

- [ ] **Full Project**
   - [ ] **TCP**
     - [ ] Connect
     - [ ] Trade Messages
   - [ ] **CLASSES**
     - [ ] com.service
       - [ ] com.service Thread
     - [ ] com.client.com.client
     - [ ] Table
     - [ ] Player??
     - [ ] Deck (*of cards*)
   - [ ] **REST**
     - [ ] **POST**
       - [ ] Create *New* Table
          - [ ] Private
          - [ ] Public
     - [ ] **GET**
       - [ ] Retrieve *Available* Public Tables
     - [ ] **PUT**
       - [ ] Update Table's Game State
       - [ ] Alter Table's Player List
         - [ ] *Add* Player
         - [ ] *Remove* Player
     - [ ] **DELETE**
       - [ ] Remove Table *if empty*

# Specification of the project 2 of SDIS-TP2

## Purpose of the Application
The group proposes to develop a cross-platform multiplayer BlackJack tables casino.

The application will have **two main screens**: a main menu screen and a table screen.
In the **main menu screen**, users will be able to **join existing tables**, or to **create new tables**.

Users are presented with the **table screen** after having joined or created one.

Each table has a maximum number of **four players** and **one dealer**.

Each table is **unique**, and therefore the cards dealt in one room will in no way influence the cards dealt of the other tables.

## Main Features

On the main menu screen there will be **three options**, each performing **one** of the following actions:

- **Join a *public* table**

A **request** will be sent to the server that will then **respond** with a list of available *public* tables with **less then 4 players** to join. The user is then able to pick a table and join the other players on the game.

- **Join a *private* table**

A **form** will be shown for the user to enter a **table ID** and **password**. If a valid combination is found the user will join the other players at the table.

- **Create a *new* table**

Users will be asked if they want to create a new *public* or *private* room. In case of the latter, an **unique table ID** should be specified along with a **password**.

## Target Platforms
 + Java Standalone application for PC/Mac

## Aditional Services and Improvements

The group will implement the basic architecture to develop a usable application. After this, the group will improve the application in order to be able to achieve a better grade.

Following are the improvements expected to be made:

-  **Architecture**

The application should follow a com.service-com.client.com.client architecture. **The dealers will be run on the com.service side** since those are the ones who deal the cards in the table and should not be available to the users to prevent cheating or tampering.

The users then communicate to the dealer of **their own table only**.

- **Scalability**

The application should scale easily. The game is restricted to 4 players a table at all the times so the only guarantee needed for scalability is the ability of the com.service to handle new instances of tables.

- **Consistency**

The application should be consistent, i.e. it should correctly manage concurrent events and user interactions. For example: when a user is prompted to make a decision they might all respond at the same time and the com.service must be able to process each of the responses to keep the game flowing correctly.

- **Fault Tolerance**

The application should tolerate faults with ease, such as temporary internet disconnections.

#### Developed by:
 + Igor Bernardo Amorim Silveira - up201505172
 + Francisco Santos - up201607928
 + Tiago Alexandre de Sousa Dias da Silva - up201404689
 + Alessandro Antoniolli - up201710790
