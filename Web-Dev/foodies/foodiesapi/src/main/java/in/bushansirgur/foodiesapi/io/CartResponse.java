package in.bushansirgur.foodiesapi.io;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

import java.util.HashMap;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CartResponse {

    private  String id;
    private String userId;
    private Map<String,Integer> items = new HashMap<>();
}
