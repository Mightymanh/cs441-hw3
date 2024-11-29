# CS 441 Fall 2024 - homework 3 - RestAPI, gRPC, and AWS Lambda
Author: Manh Phan

Email: dphan8@uic.edu 

Youtube demo Deploying app in AWS: []()

## About
This project is a more complicating version of an HTTP conversation where a user (or client) sends a prompt to server and server returns the response. 
The prompt navigates through 2 servers (via HTTP requests and RPC) and some AWS services to reach a genAI model where response text is generated and sends back in the reverse route to the user.

## Implementation
![image](https://github.com/user-attachments/assets/210cd3ec-1cbe-4c83-acd1-0966a1a80c88)

There are 3 main classes in this project (also corresponds to 3 files in the /src/main/scala folder):

- **FinchServer**: a RESTful Server that receives HTTP requests from the client. The Server uses Finch/Finagle Framework which is developed by Twitter.
  After receiving a prompt from the client, the prompt will be forwarded to **GrpcServer** through gRPC.
- **GrpcServer**: a server that receives gRPC call from **FinchServer**. The call input is the prompt from the client.
  The prompt is then sent to Amazon API Gateway through HTTP POST request to invoke AWS Lambda function **BedrockLambda**.
- **BedrockLambda**: lambda function that receives prompt from **GrpcServer**.
  It passes the prompt to **Amazon Bedrock model** (for this project I use Jurassic-2 Mid model from AI21) to generate response. The response then pass back to **GrpcServer** -> **FinchServer** -> **client**.

Client here can be browser, cURL, Postman. I use cURL for this project. Some of the example cURL commands that I test are in the src/main/resources/input.txt

## Deployment
**FinchServer** and **GrpcServer** are both deployed in an EC2 instance. **BedrockLambda** is added to AWS Lambda function which connects to Amazon API Gateway as a front door to receive HTTP requests.

## How to run this project in your laptop (or any computer)
### 1. Setup
Open this project in any of your favorite IDEA that supports Scala Project. My favorite IDEA is IntelliJ where I write code for this project. A terminal also works.
I use terminal to run my project because I need to run two things.
This project runs on **Scala 2.12** and **Java 11** so make sure you have scala 2.12 and java 11 to run this project.

### 2. Run the app
**BedrockLambda** is already deployed by me in the AWS Lambda. So you only have to run **FinchServer** and **GrpcServer**. At the root of the project compile using the command:
`sbt compile`. Once the project is compiled, open two terminal windows. Type `sbt run` in both windows and enter to run command. Then in one window, choose **FinchServer**.
In other window, choose **GrpcServer**. Any order is good because both servers run indepedently and they communicate each other through gRPC.

By default **FinchServer** is running in localhost:8080 and **GrpcServer** is running in localhost:50051. The default ports are specifed in the application.conf in src/resources.
You can change the port for each server if you want. Below is a more general command to run each class:

- Running **FinchServer**:
```
  sbt run <FinchServer-port (optional, default=8080)> <GrpcServer-port (optional, default=50051)> <GrpcServer-host (optional, default="localhost")>
```
- Running **GrpcServer**:
```
  sbt run <GrpcServer-port (optional, default=50051)>
```

IMPORTANT NOTE: Make sure the **GrpcServer-port** that you type for running FinchServer matches with the **GrpcServer-port** you type for running GrpcServer.
And the **GrpcServer-host** that you type for running FinchServer must match with the **host address / ip address** for where the GrpcServer is run. 
We want FinchServer to communicate with GrpcServer.

## Testing app
![image](https://github.com/user-attachments/assets/f08201c7-1b49-4a48-ad24-f930b38af8e5)

There are two endpoints in the app that you can send http request to:

**GET /hello**: To test whether the app (or the FinchServer) can say hello back to us.

```
  curl localhost:8080/hello
```

**POST /prompt**: To test it with a prompt and the app returns a response. Replace your prompt with the **PUT-YOUR-PROMPT-HERE** in the command below

```
  curl -X POST -H "Content-Type: application/json" -d '{"prompt":"<PUT-YOUR-PROMPT-HERE>"}' localhost:8080/prompt
```

I put the example cURL commands in the /src/main/resources/input.txt so you can quickly grab and test.

## Additional commands
`sbt clean`: clean project

`sbt test`: run test. There are 5 tests in the **src/test/** directory
