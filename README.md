##Body parser sample application##

This is an application modified from the sample from Chapter 10 of
[Play for Scala](http://bit.ly/playscala), demonstrating how to write a
custom BodyParser.

Instead of Java libraries[*], [S3Put](https://github.com/cfchou/s3put)
is used here. S3Put is a small library that uploads(HTTP PUT) files to Amazon
S3 using _Spray.io_ and _Akka_.


[*] Original sample uses the java library [AsyncHttpClient(AHC)]
(https://github.com/AsyncHttpClient/async-http-client) together with
[Grizzly](https://grizzly.java.net/) provider and a FeedableBodyGenerator.







