package com.kornfeld.notifier;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class NotifierApplication {

	public static void main(String[] args) {
		SpringApplication.run(NotifierApplication.class, args);
		System.out.println("Start");
	}

	/*	Test curl:
	curl --location --request POST 'http://localhost:8080/sendChanges' \
	  --header 'Content-Type: multipart/form-data' \
	  --form "Branch=main" \
	  --form "ApiChanges=TheApiChanges" \
	  --form "IsBreakingChange=true" \
	  --form "Repository=ExampleRepo" \
	  --form "Token=myToken" \
	  --form "title=Automated email for breaking changes in ExampleRepo repository with branch name main." \
	  --form "header=This email was automatically sent by the GitHub Action workflow MyWorkflow. The commit hash was: 23848hdsf88232." \
	  --form "File=@local/localHTML.html"
	 */

	/* Email test curl:
	curl -X POST -d "title=TestTitle&body=This is the body&toEmail=d.kornfeld11@gmail.com" http://localhost:8080/sendTest
	 */

}
