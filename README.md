
# NFL stats
Little sample app for playing around with GraphQL paging

## Requirements:

### SBT
The build tool used by this project. Download the latest version [here](https://www.scala-sbt.org/download.html).
Alternatively, the link includes instructions for installing it with a package manager.
Follow instructions for java version

### Java
Please use java version 11

### Docker
You need docker installed locally as well as the local kubernetes cluster working in order for the local setup to work

### Skaffold
The tool used for running the application in your local k8s cluster. Please install the version [here](https://github.com/GoogleContainerTools/skaffold/releases/tag/v1.12.1).
The reason for using the old version is because for convenience the behaviour of the local setup mirrors how k8s
actually behaves in production: the initialization scripts are not configured to wait upon the pods they are initializing,
they simply try to run and fail if their respective pods are not yet available. Newer version of skaffold would cause
the local run to fail right after the first failure, which is undesirable. There is a ticket on the skaffold repo 
for making this behavior configurable, but it is not yet implemented.

The kubernetes manifests for the local deployment can be found in the `/k8s` directory.

### IntelliJ Idea
Not a requirement per se, but it would make it easier to explore the code and run the unit tests.
Make sure you have the scala plugin installed.

## Running locally 
Use
```skaffold dev --port-forward=true``` or ```skaffold dev --port-forward=true --no-prune=false --cache-artifacts=false```
for running locally. The latter command ensures all images are rebuilt from scratch 

### What APIs are available? 
If you are using [Postman](https://www.postman.com/),
then you can find a postman collection listing the available requests to the application.

For the graphQL API specifically, the app serves [GraphiQL](https://github.com/graphql/graphiql)
at `http://localhost:9000/nfl`: it is a web based UI that allows exploring the GraphQL schema and firing off manual requests.

### /etc/hosts
Add
```
127.0.0.1 s3-mock.default.svc.cluster.local
```
to your `/etc/hosts` in order to be able to use the download export files API. This is done in order to allow downloading the export files through your browser or Postman.
Make sure Postman is configured to follow redirects.

### How do I run the tests
Run `sbt` in the project's main directory. Once the SBT console loads, running `frontend/test` or `exportGenerator/test`
would run the tests for the corresponding subproject

### How do I get data in my database

RushingStatsRepositoryTest loads the entire rushing.json to the local db pod spinned up by skaffold before tests run, so 
the simplest thing you could do is put a breakpoint in any of the test, debug and terminate the test before the 
test finishes (and therefore skip db cleanup)


## Application Architecture
The application has the following dependencies:
1. MySQL
    Used as the data store for the rusing stats imported from `rushing.json`, as well as the table tracking the 
    existing exports.
2. RabbitMQ
    This is a message broker used to communicate between the application's deployments
3. S3 compatible storage
    Used for storing rushing stat csv exports
   
The app consists of two separate deployments: 
1.
   The `frontend` service, which exposes a GraphQL API.
   Check GraphiQL for the exact operation names / types available. In general, the API allows for querying paged data 
   from the database and synchronously returning it to the caller (to be consumed by an UI for example). 
   Also, it is possible to export the result of a selection to a file (that is, the entire dataset specified, without paging).
   The export is triggered asynchronously, through a GraphQL mutation. This publishes a message in RabbitMQ with the export
   request and creates an entry in the `exports` table allowing to monitor the export progress (by a few simple statuses).
   The `frontend` deployment listens to another RabbitMQ queue for status updates to the exports requested.
   When an export's status is updated to `SUCCESSFUL`, its file becomes available to download via an HTTP api 
   that redirects to S3.
   
   The authentication for the APIs is very simple: it assumes that a user UUID is present in the user_uuid header
   of each incoming request, and that UUID is used as the uuid of the user, in order to determine what they have access
   to. In a real application, a JWT would be most likely used. Authentication is needed in order
   to determine that a user has access to the results of a particular export for viewing / downloading.

2. 
    The `exportGenerator` service, which listens to export generation requests from a queue and executes them.
    The stats data is streamed from the database to S3 and upon completion an export status update is sent back to the 
    `frontend` service

The scala code is separated in 3 sub-projects: one per deployment, as well as one for common dependencies 

No UI is currently implemented, but the GraphiQL test page demonstrates how we can serve a a web application.
