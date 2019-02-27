## Design and implementation

### Phase one

I have noticed that I will require implement some error handling logic.
I do not like error handling purely on exception basis. The reason is exceptions do not appear
in the return type and are not statically checked. Therefore they are side-effects and are hard too keep track where we can except an exception that
we need to handle. In large systems this leads to state where most methods will have few lines of effective code and then
defensive checks for all possible exceptions which at some cases cannot even happen. It is similar situation to nulls in Java and the reason Kotlin have added them to
its type system. Therefore, I have added core of Arrow library which contains the most basic data types for
functional error handling (such as Option, Either and Try) and used them in BillingService when interacting with
remote system.

I would like to note that I am not against exceptions when used as short-circuiting mechanism when dealing with
unrecoverable and/or not domain errors. I understand that purely functional programming is sometimes too much and
therefore it is all right for me to have some global exception handler which handles all these exceptions by logging them
and making sure that they won't crash the application. However, there is way to remove exceptions completely with use of
some effect which implements MonadError, such as IO in Scala or Haskell).
  

### Phase two

After I have implemented initial version of BillingService which could handle required error states.
I was thinking about executing service which could run a task periodically. I have decided that I will
split this periodic execution to problem domains. First, I need to describe the scheduling of these tasks and
afterwards I need handle the execution at the scheduled intervals.

I decided that I want to use Coroutines as the main way to handle asynchronous code. The reason is
that they I found the asynchronous code more readable when using CPS. Furthermore, they are officially supported and as 1.3
not experimental. However, I had very little experience with them so I needed to learn a bit about them.
In previous projects we mostly handled asynchronous code using RxJava or Futures.

Since Coroutines allows to create infinite sequences of values which are lazily computed, I have decided
that I will use this capability to implement Schedule as a Sequence of ZonedDateTimes.

For the execution I have then used simple suspendable lambda as an action which can executed as times defined by
the ZonedDateTime. The execution engine is implemented using Coroutines as well. I have created separate
dispatcher for the scheduling in order to reduce latency if needed by increasing the priority of scheduling threads.
For the execution of actions I have decided to use unbounded thread pool as we can expect that the operation running
inside the actions will block.

### Phase three

As third part I have extended BillingService to communicate with persistence layer so it can update states of invoices.
I tried to add some very simple error recovery mechanism when we actually charge the customer but our database is in some
inconsistent state. This recovery mechanism could be extracted somewhere else and work on more abstract layer. Furthermore,
I changed the API of billing service as I wanted to add some possibility to process the paid invoices, for example by notification service.

### Phase four

I added concept of use case which represents single capability/domain logic of the system. This is also where the threading is handled.
This approach is very popular in Android space and is popularized by Robert C. Martin in his book Clean Architecture. However,
this is just one of the approaches which we can implement.

### What can be improved

Well, everything can be improved :). However, I wanted to list some things I did not add and I would like to.

1. Add global error handling by adding some global handler either as part of Coroutines or using ExecutionService.
2. Add exponential backoff for network failure.
3. Add integration tests and solve how to do time shifts in unit tests with Coroutines.
4. Improve logging. I have added some logs but I understand they are lacking. First, define consistent logging strategy which
we can use for indexing of the logs for some external analytics such as ELK stack.
5. Add configuration. We can use configuration to specify amount of threads in individual thread pool, their priority, logging, operational hours of payment providers...
6. Implement the notification service. We may want to contact a customer when the billing was successful or when it fails due to wrong account/insufficient funds. We 
also might want to contact management/administrator when critical errors happens, etc.
7. Improve modularity by encapsulating code in modules using internal visibility.
8. Add possibility to receive updates from scheduled jobs.

## Antaeus

Antaeus (/ænˈtiːəs/), in Greek mythology, a giant of Libya, the son of the sea god Poseidon and the Earth goddess Gaia. He compelled all strangers who were passing through the country to wrestle with him. Whenever Antaeus touched the Earth (his mother), his strength was renewed, so that even if thrown to the ground, he was invincible. Heracles, in combat with him, discovered the source of his strength and, lifting him up from Earth, crushed him to death.

Welcome to our challenge.

## The challenge

As most "Software as a Service" (SaaS) companies, Pleo needs to charge a subscription fee every month. Our database contains a few invoices for the different markets in which we operate. Your task is to build the logic that will pay those invoices on the first of the month. While this may seem simple, there is space for some decisions to be taken and you will be expected to justify them.

### Structure
The code given is structured as follows. Feel free however to modify the structure to fit your needs.
```
├── pleo-antaeus-app
|
|       Packages containing the main() application. 
|       This is where all the dependencies are instantiated.
|
├── pleo-antaeus-core
|
|       This is where you will introduce most of your new code.
|       Pay attention to the PaymentProvider and BillingService class.
|
├── pleo-antaeus-data
|
|       Module interfacing with the database. Contains the models, mappings and access layer.
|
├── pleo-antaeus-models
|
|       Definition of models used throughout the application.
|
├── pleo-antaeus-rest
|
|        Entry point for REST API. This is where the routes are defined.
└──
```

## Instructions
Fork this repo with your solution. We want to see your progression through commits (don’t commit the entire solution in 1 step) and don't forget to create a README.md to explain your thought process.

Happy hacking 😁!

## How to run
```
./docker-start.sh
```

## Libraries currently in use
* [Exposed](https://github.com/JetBrains/Exposed) - DSL for type-safe SQL
* [Javalin](https://javalin.io/) - Simple web framework (for REST)
* [kotlin-logging](https://github.com/MicroUtils/kotlin-logging) - Simple logging framework for Kotlin
* [JUnit 5](https://junit.org/junit5/) - Testing framework
* [Mockk](https://mockk.io/) - Mocking library
