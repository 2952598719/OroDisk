package top.orosirian.orodisk.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchResult {

    private Long fileId;

    private String fileName;

    private String snippet;

}
