package com.vmataj.travel;

import jakarta.persistence.*;

@Entity
class SavedDestination {
    @Id @Column(length = 65) String id;
    @Column(nullable = false, length = 40) String owner;
    @Embedded Destination destination;

    protected SavedDestination() {}

    SavedDestination(String owner, Destination destination) {
        this.id = key(owner, destination.placeId());
        this.owner = owner;
        this.destination = destination;
    }

    static String key(String owner, long placeId) {
        return owner + ":" + placeId;
    }
}
