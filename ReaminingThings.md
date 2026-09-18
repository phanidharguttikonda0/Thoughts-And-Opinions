
# Remaining Things to do in Thoughts Service and Identity Service

## Identity Service
-> In Identity Service for a follow , need to send a notification to other user via kafka.

## Thoughts Service
-> In Thoughts Service, need to send the new thought and list of followers in to kafka, to
update the followers feed in redis.

-> after thought repost or opinion on a thought , need to send a notification via kafka,
to that user of parent thought id.

-> add a field in create thought to pass media files and add a field in response thought or
opinions to get the related media file urls.


## API Gateway Implementation is Pending, While Implementing API Gateway going resolve, media file urls thing.

## After API Gateway Implementation, going to implement Timeline Service and designing kafka and notification service.