package org.lovesoa.secondservice.dto.pagable;

import lombok.Data;

@Data
public class SortDTO {
    private boolean sorted;
    private boolean unsorted;
    private boolean empty;
}
