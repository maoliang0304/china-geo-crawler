package china.geo.crawler;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 地理位置model.
 *
 * @author maoliang0304
 * @version 1, 2019/2/25
 */
@Data
@Accessors(chain = true)
public class Geo {

    /**
     * 1:省、2：市、3：区.
     */
    private Integer level;
    /**
     * 上级地理位置code.
     */
    private Integer parentCode;
    /**
     * 地理位置code.
     */
    private Integer code;
    /**
     * 地理位置名称.
     */
    private String name;

}
