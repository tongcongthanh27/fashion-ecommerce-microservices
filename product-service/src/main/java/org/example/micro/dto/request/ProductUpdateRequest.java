package org.example.micro.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductUpdateRequest {

    @Size(max = 255, message = "Tên sản phẩm không được vượt quá 255 ký tự")
    @Pattern(regexp = ".*\\S.*", message = "Tên sản phẩm không được để trống")
    private String name;

    @Min(value = 0, message = "Giá sản phẩm phải lớn hơn hoặc bằng 0")
    private Integer price;

    @Min(value = 0, message = "Số lượng tồn kho phải lớn hơn hoặc bằng 0")
    private Integer stock;

    private String categoryId;
}
