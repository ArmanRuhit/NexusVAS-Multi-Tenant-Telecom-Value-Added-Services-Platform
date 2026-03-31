package dev.armanruhit.nexusvas.common_lib.exception;

public class EntityNotFoundException extends NexusVasException{

    public EntityNotFoundException(String entity, String id) {
        super("%s not found with id: %s".formatted(entity, id), "ENTITY_NOT_FOUND");
    }
    
}
