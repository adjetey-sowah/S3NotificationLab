package com.giftedlabs;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class S3UploadHandler implements RequestHandler<Object,String> {

    // Get the SNS Topic ARN from environment variables (set by SAM template)
    private static final String SNS_TOPIC_ARN = System.getenv("SNS_TOPIC_ARN");
    private static final String ENVIRONMENT = System.getenv("ENVIRONMENT");
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String handleRequest(Object input, Context context) {
        try {
            JsonNode root = mapper.readTree(mapper.writeValueAsString(input));
            JsonNode record = root.get("Records").get(0);
            String bucketName = record.get("s3").get("bucket").get("name").asText();
            String objectKey = URLDecoder.decode(
                    record.get("s3").get("object").get("key").asText(), StandardCharsets.UTF_8
            );

            String message = "[%s] File %s has been uploaded to bucket %s.".formatted(ENVIRONMENT,objectKey, bucketName);
            context.getLogger().log("Sending message: "+message+ "\n");

            try(SnsClient snsClient = SnsClient.create()){
                PublishRequest publishRequest = PublishRequest.builder()
                        .topicArn(SNS_TOPIC_ARN)
                        .subject("S3 File Upload Notification")
                        .message(message)
                        .build();

                PublishResponse publishResponse = snsClient.publish(publishRequest);
                context.getLogger().log("Message published with ID: "+publishResponse.messageId()+"\n");
            }

            return "Notification sent successfully";
        }
        catch (Exception e){
            context.getLogger().log("Failed: "+e.getMessage()+"\n");
            return "Notification failed";
        }
    }
}