package com.hybridmode.domain.shared

abstract class AggregateRoot {
    private val domainEvents: MutableList<DomainEvent> = mutableListOf()

    protected fun record(event: DomainEvent) {
        domainEvents += event
    }

    fun pullDomainEvents(): List<DomainEvent> {
        val copy = domainEvents.toList()
        domainEvents.clear()
        return copy
    }
}

interface ValueObject

interface DomainEvent {
    val occurredAtEpochMs: Long
}

/** Collects events from aggregates after a successful unit of work. */
interface DomainEventPublisher {
    fun publish(events: List<DomainEvent>)
}

class LoggingDomainEventPublisher : DomainEventPublisher {
    override fun publish(events: List<DomainEvent>) {
        // Infrastructure may replace this with outbox / analytics.
        events.forEach { event ->
            println("[domain-event] ${event::class.simpleName}: $event")
        }
    }
}
