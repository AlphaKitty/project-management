SELECT id,
    rule_id,
    user_id,
    todo_id,
    project_id,
    email_type,
    recipient_email,
    template_code,
    template_variables,
    priority,
    scheduled_time,
    status,
    retry_count,
    max_retries,
    error_message,
    sent_time,
    create_time,
    update_time
FROM email_send_queue
WHERE (
        status = 'PENDING'
        AND scheduled_time <= '2025-06-21T09:00:18.685'
    )
ORDER BY priority DESC,
    create_time ASC
LIMIT 50