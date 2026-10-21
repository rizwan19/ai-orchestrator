-- Composite index serves cursor pagination ordered by (created_at, id); built before dropping the narrower one.
create index message_conversation_created_id on message (conversation_id, created_at, id);

drop index message_conversation_created;
