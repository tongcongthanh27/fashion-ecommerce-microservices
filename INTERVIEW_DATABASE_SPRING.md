# Ôn phỏng vấn Backend Java: Database, Spring và SOLID

Tài liệu dành cho việc **hiểu bản chất, liên hệ project và diễn đạt khi phỏng vấn**. Mỗi chủ đề có phần giải thích, cách lựa chọn và câu trả lời mẫu. Không cần học thuộc nguyên văn; hãy nhớ vấn đề cần giải quyết và lý do chọn giải pháp.

## Cách đọc và phạm vi dẫn chứng

- **[Code hiện có]**: đã đọc thấy trong source của bạn.
- **[Đề xuất]**: phương án cải tiến hoặc ví dụ mở rộng, chưa phải tính năng đã triển khai.
- **[Minh họa]**: code/SQL để giải thích lý thuyết; không phải bản vá có thể chép vào chạy ngay.

Hai project được đối chiếu:

- `MiCROSERVICE`: ưu tiên `product-service`, `order-service`, `api-gateway`.
- `ecomerc shop/ecommerc_shop_backend`: bổ sung các ví dụ thực tế về optimistic lock, coupon lock, fetch join và entity graph.

Các `pom.xml` được đọc khai báo Spring Boot `4.1.1` và driver MySQL. Vì chưa kết nối database để kiểm tra engine, schema và version đang chạy, SQL bên dưới **giả định MySQL 8+ với InnoDB**; những khác biệt PostgreSQL được ghi riêng. Có driver hoặc dependency chưa chứng minh tính năng đang được sử dụng. Tài liệu chỉ phân tích source, chưa đo query, chưa chạy concurrency test, không khẳng định một lỗi hiệu năng đã tái hiện.

Các liên kết code trỏ đến file trong hai thư mục project hiện tại; vị trí dòng ghi trong lời dẫn là vị trí lúc biên soạn. Các đoạn code rút gọn có thể bỏ annotation/import không liên quan.

## Mục lục

1. [ACID và phạm vi transaction](#1-acid-và-phạm-vi-transaction)
2. [Chuẩn hóa 3NF và phi chuẩn hóa](#2-chuẩn-hóa-3nf-và-phi-chuẩn-hóa)
3. [JOIN và subquery](#3-join-và-subquery)
4. [Pattern thiết kế dữ liệu](#4-pattern-thiết-kế-dữ-liệu)
5. [Index: bản chất, loại và tradeoff](#5-index-bản-chất-loại-và-tradeoff)
6. [Phân tích SQL chậm bằng execution plan](#6-phân-tích-sql-chậm-bằng-execution-plan)
7. [Transaction isolation](#7-transaction-isolation)
8. [Pessimistic lock và optimistic lock](#8-pessimistic-lock-và-optimistic-lock)
9. [Pagination](#9-pagination)
10. [Partitioning và sharding](#10-partitioning-và-sharding)
11. [N+1 query trong JPA](#11-n1-query-trong-jpa)
12. [Trigger và CTE](#12-trigger-và-cte)
13. [IoC, DI và constructor injection](#13-ioc-di-và-constructor-injection)
14. [Bean scope và vòng đời bean](#14-bean-scope-và-vòng-đời-bean)
15. [AOP, proxy và self-invocation](#15-aop-proxy-và-self-invocation)
16. [Transactional: propagation, rollback và ranh giới](#16-transactional-propagation-rollback-và-ranh-giới)
17. [Request lifecycle, Filter và Interceptor](#17-request-lifecycle-filter-và-interceptor)
18. [Spring Boot và auto-configuration](#18-spring-boot-và-auto-configuration)
19. [JPA: lifecycle, dirty checking, save và flush](#19-jpa-lifecycle-dirty-checking-save-và-flush)
20. [SOLID, đặc biệt Dependency Inversion](#20-solid-đặc-biệt-dependency-inversion)
21. [Kể về project khi phỏng vấn](#21-kể-về-project-khi-phỏng-vấn)
22. [Các câu dễ trả lời sai](#22-các-câu-dễ-trả-lời-sai)
23. [Danh sách dẫn chứng trong source](#23-danh-sách-dẫn-chứng-trong-source)

---

## 1. ACID và phạm vi transaction

### 1.1. Transaction là gì?

Transaction gom nhiều thao tác database thành một đơn vị công việc. Ví dụ tạo đơn hàng thường gồm lưu đơn, lưu các dòng hàng và cập nhật tồn kho. Nếu chúng cùng nằm trong một transaction của một database thì có thể commit hoặc rollback cùng nhau.

### 1.2. Bốn tính chất ACID

**Atomicity — tính nguyên tử:** các thay đổi trong transaction thành công cùng nhau hoặc được hủy cùng nhau. Nếu lưu được đơn nhưng lưu dòng hàng lỗi, rollback giúp không để lại đơn dở dang.

**Consistency — tính nhất quán:** transaction hợp lệ đưa dữ liệu từ trạng thái thỏa các quy tắc sang trạng thái tiếp tục thỏa quy tắc. Ví dụ khóa ngoại hợp lệ, mã đơn không trùng, số lượng không âm. Database chỉ bảo đảm những ràng buộc nó biết; nghiệp vụ phải được biểu diễn bằng constraint, SQL hoặc logic ứng dụng đúng. Thêm `@Transactional` không tự phát hiện mọi vi phạm nghiệp vụ.

**Isolation — tính cô lập:** quy định các transaction chạy đồng thời được nhìn thấy và ảnh hưởng nhau như thế nào. Không có nghĩa mọi isolation level đều cho kết quả giống chạy tuần tự.

**Durability — tính bền vững:** sau khi commit được xác nhận theo cấu hình lưu bền, thay đổi có thể phục hồi sau sự cố. Database dùng transaction log và cơ chế recovery; mức bảo đảm còn liên quan cấu hình flush log, phần cứng và replication. Không nên diễn đạt thành “commit là không bao giờ mất dữ liệu trong mọi loại sự cố”.

### 1.3. Dẫn chứng trong project

**[Code hiện có]** [OrderTransactionService.java](../ecomerc%20shop/ecommerc_shop_backend/src/main/java/org/example/ecommerc_shop/service/impl/OrderTransactionService.java), method `createOrderTransaction()` từ dòng 45:

- Tạo `Order` và `OrderItem`.
- Trừ `ProductVariant.quantityInStock`.
- Tăng `Coupon.usedCount` nếu có coupon.
- Đánh dấu các `CartItem` đã xóa và thêm `TrackingLog`.
- Bao nghiệp vụ trong `@Transactional`.

Đây là ví dụ tốt để nói về atomicity **trong database của shop**. Nếu transaction rollback do lỗi cập nhật, những thay đổi DB thuộc transaction đó được hủy. Việc chống tranh chấp tồn kho còn cần `@Version`, lock hoặc SQL có điều kiện; atomicity riêng lẻ chưa đủ.

**[Code hiện có]** Microservice có `OrderServiceImpl.createOrder()` lưu DB rồi gọi `kafkaTemplate.send(...)`. Không thể suy ra DB commit và Kafka publish là một transaction nguyên tử chỉ vì method có `@Transactional`. DB rollback không thu hồi một message đã gửi thành công; ngược lại, DB có thể commit trong khi gửi message thất bại. `send()` còn trả kết quả bất đồng bộ.

**[Đề xuất]** Transactional Outbox: lưu đơn và một bản ghi sự kiện outbox trong cùng DB transaction; worker/CDC publish sau. Consumer xử lý idempotent theo `eventId` hoặc khóa nghiệp vụ vì message có thể được gửi lại. Đây là cách kết nối tính nguyên tử cục bộ với nhất quán cuối cùng giữa các service. [Đọc thêm: Debezium Outbox Event Router](https://debezium.io/documentation/reference/stable/transformations/outbox-event-router.html).

### Nói khi phỏng vấn

> “ACID là nguyên tử, nhất quán, cô lập và bền vững. Trong project shop, em đặt lưu đơn, dòng hàng, trừ kho và cập nhật coupon trong một transaction. Với microservice, transaction DB không tự bao phủ Kafka hay HTTP nên em cần cơ chế như outbox và xử lý idempotent để tránh lệch trạng thái giữa các service.”

## 2. Chuẩn hóa 3NF và phi chuẩn hóa

### 2.1. Vì sao cần chuẩn hóa?

Giả sử mỗi sản phẩm đều lưu `category_id`, `category_name`, `category_description`. Một danh mục có 1.000 sản phẩm thì tên danh mục bị lặp 1.000 lần.

Hệ quả là **update anomaly**: đổi tên thiếu vài dòng; **insert anomaly**: khó tạo danh mục chưa có sản phẩm; **delete anomaly**: xóa sản phẩm cuối cùng có thể làm mất thông tin danh mục nếu chỉ lưu thông tin ở đó.

Chuẩn hóa tổ chức các sự thật về đúng nơi của chúng để giảm các bất thường này.

### 2.2. 1NF, 2NF và 3NF theo cách dễ hiểu

**1NF:** mỗi ô chứa một giá trị theo miền dữ liệu đã chọn; tránh nhóm cột lặp `phone1`, `phone2`, `phone3` hay chuỗi danh sách ID để thay cho một quan hệ. Ví dụ các dòng hàng nên nằm trong `order_items`, không nhét toàn bộ danh sách sản phẩm vào một chuỗi phân cách bằng dấu phẩy.

**2NF:** đã đạt 1NF và không có thuộc tính không khóa phụ thuộc vào chỉ một phần của một khóa ứng viên ghép. Ví dụ khóa `(order_id, product_id)`: `quantity` thuộc cả dòng hàng, nhưng `product_name` chỉ phụ thuộc `product_id`, nên thông tin tên sản phẩm hiện tại nên ở bảng sản phẩm.

**3NF:** đã loại phụ thuộc không phù hợp giữa các thuộc tính không khóa. Ví dụ `product_id -> category_id -> category_name`, nên tách `category_name` ra bảng `categories`.

Phát biểu chính xác hơn: với mỗi phụ thuộc hàm không tầm thường `X -> A`, `X` là siêu khóa hoặc `A` là thuộc tính thuộc ít nhất một khóa ứng viên. Câu “mọi cột chỉ phụ thuộc khóa chính” là mẹo nhớ, không bao quát các trường hợp nhiều khóa ứng viên.

### 2.3. Dẫn chứng chuẩn hóa trong project

**[Code hiện có]** [Product.java](product-service/src/main/java/org/example/micro/entity/Product.java) có `category_id` qua `@ManyToOne`, còn tên danh mục ở [Category.java](product-service/src/main/java/org/example/micro/entity/Category.java).

Đây là ví dụ tách dữ liệu theo quan hệ và tránh lặp tên danh mục. Chỉ nhìn một vài entity chưa đủ chứng minh toàn bộ schema đạt 3NF; còn phải xét khóa ứng viên và phụ thuộc hàm theo nghiệp vụ.

### 2.4. Phi chuẩn hóa dùng khi nào?

Phi chuẩn hóa chủ động lưu dữ liệu lặp hoặc kết quả tính sẵn để giảm chi phí đọc. Ví dụ lưu `orders.total_amount` thay vì mỗi lần đều cộng các dòng hàng, hoặc xây bảng báo cáo doanh thu theo ngày.

Đổi lại phải trả lời: ai cập nhật dữ liệu dẫn xuất, cập nhật cùng transaction hay bất đồng bộ, chấp nhận trễ bao lâu, và khôi phục thế nào khi bị lệch? Đọc nhanh hơn nhưng ghi, đồng bộ và kiểm tra nhất quán phức tạp hơn.

Nên bắt đầu bằng mô hình dễ bảo toàn dữ liệu; phi chuẩn hóa khi có workload và đo đạc cho thấy cần thiết. Không có quy tắc “nhiều JOIN thì cứ phi chuẩn hóa”.

### 2.5. Snapshot lịch sử khác với sao chép dữ liệu hiện tại

**[Code hiện có]** Microservice lưu `OrderItem.price = productDTO.getPrice()` lúc đặt hàng. Shop có `OrderItem.unitPrice`, và [OrderItemMapper.java](../ecomerc%20shop/ecommerc_shop_backend/src/main/java/org/example/ecommerc_shop/mapper/OrderItemMapper.java) ánh xạ từ `productVariant.price`.

Giá tại thời điểm mua là **một sự thật lịch sử riêng**. Nó phải giữ nguyên khi giá sản phẩm hiện tại thay đổi. Không nên “chuẩn hóa” bằng cách bỏ giá dòng hàng rồi luôn JOIN giá mới nhất. Snapshot này có bề ngoài giống dữ liệu lặp nhưng mang ý nghĩa nghiệp vụ khác.

`Order.totalAmount` trong microservice là ví dụ dữ liệu tổng hợp được lưu sẵn. Vì service đã lưu tổng, những luồng thay đổi dòng hàng sau này phải có quy tắc giữ tổng đúng.

### Nói khi phỏng vấn

> “Em dùng chuẩn hóa để giảm dữ liệu lặp và tránh bất thường khi cập nhật. Phi chuẩn hóa phù hợp khi cần tối ưu đọc và có cách đồng bộ rõ ràng. Trong project, sản phẩm tham chiếu danh mục bằng khóa ngoại; giá trên dòng đơn hàng được giữ như snapshot lịch sử, còn tổng tiền là giá trị tính sẵn cần bảo toàn khi cập nhật.”

## 3. JOIN và subquery

### 3.1. Các loại JOIN thường gặp

**INNER JOIN** lấy những dòng có quan hệ khớp ở cả hai phía. Sản phẩm chưa có danh mục sẽ không xuất hiện nếu dùng inner join với danh mục.

**LEFT JOIN** giữ mọi dòng bên trái; nếu không có dòng bên phải thì các cột bên phải là `NULL`. Phù hợp danh sách sản phẩm vẫn hiển thị sản phẩm chưa phân loại.

**RIGHT JOIN** tương tự left join khi đổi hai phía; thường viết lại bằng left join để dễ đọc. **FULL OUTER JOIN** giữ cả hai phía, nhưng MySQL không có cú pháp trực tiếp cho loại này. **CROSS JOIN** tạo mọi cặp, dễ làm số dòng tăng rất lớn nếu dùng nhầm.

**[Minh họa theo schema microservice]**

```sql
SELECT p.id, p.name, c.name AS category_name
FROM products p
LEFT JOIN categories c ON c.id = p.category_id
WHERE p.is_deleted = false;
```

Bẫy thường gặp: thêm `WHERE c.is_deleted = false` sẽ loại cả dòng không có danh mục vì điều kiện với `NULL` không đúng. Nếu muốn giữ mọi sản phẩm nhưng chỉ nối danh mục còn hiệu lực, đặt điều kiện này trong `ON`.

JOIN quan hệ một-nhiều làm lặp thông tin dòng cha. JOIN `orders` với `order_items` rồi `COUNT(*)` là đếm số dòng sau join, không mặc nhiên là số đơn. Có thể dùng `COUNT(DISTINCT o.id)` hoặc aggregate trước tùy yêu cầu.

### 3.2. Subquery, EXISTS và IN

Subquery là một truy vấn nằm trong truy vấn khác. Có thể trả một giá trị, một tập giá trị hoặc một bảng dẫn xuất. Correlated subquery tham chiếu cột của truy vấn ngoài.

Ví dụ cần tìm đơn có ít nhất một dòng hàng số lượng từ 2 trở lên:

```sql
SELECT o.id
FROM orders o
WHERE EXISTS (
    SELECT 1
    FROM order_items oi
    WHERE oi.order_id = o.id
      AND oi.quantity >= 2
);
```

`EXISTS` diễn đạt bài toán tồn tại và không làm nhân bản dòng đơn như một join trực tiếp. `IN` phù hợp kiểm tra thuộc tập kết quả. Cẩn thận `NOT IN`: nếu tập con chứa `NULL`, biểu thức có thể trả unknown khiến kết quả khác mong muốn; `NOT EXISTS` thường dễ diễn đạt việc “không có dòng liên quan” hơn.

Không kết luận “JOIN luôn nhanh hơn subquery” hoặc “correlated subquery chắc chắn chạy riêng N lần”. Optimizer có thể biến đổi thành semijoin, materialize hoặc chọn kế hoạch khác. Cần execution plan và dữ liệu thật.

### 3.3. Dẫn chứng trong project

**[Code hiện có]** Shop có JPQL `JOIN FETCH` ở `CartItemRepository.findCartItemsWithProductVariant()` để tải `CartItem`, `ProductVariant`, `Product`. Phân biệt: SQL JOIN kết hợp dòng; JPQL fetch join còn yêu cầu ORM tải association vào entity. JOIN thông thường trong JPQL không tự bảo đảm association đã được khởi tạo.

### Nói khi phỏng vấn

> “Em chọn JOIN khi cần lấy cột từ các bảng liên quan, chọn EXISTS khi chỉ kiểm tra có quan hệ thỏa điều kiện. Em chú ý số dòng bị nhân lên ở quan hệ một-nhiều và vị trí điều kiện với LEFT JOIN. Hiệu năng phải xác nhận bằng execution plan, không dựa vào quy tắc JOIN luôn nhanh hơn subquery.”

## 4. Pattern thiết kế dữ liệu

### 4.1. EAV — Entity, Attribute, Value

EAV biểu diễn mỗi thuộc tính thành một dòng thay vì một cột cố định. Ví dụ bảng `product_attribute_values(product_id, attribute_id, value)` có các dòng `(P1, RAM, 16GB)`, `(P1, COLOR, black)`.

**Ưu điểm:** thêm loại thuộc tính mà ít thay schema; phù hợp danh mục hàng có nhiều thuộc tính thưa và thay đổi theo ngành hàng.

**Đánh đổi:** lọc nhiều thuộc tính cần nhiều join/aggregate; nếu tất cả giá trị là chuỗi thì so sánh số, ngày và validation khó; constraint và thống kê optimizer kém rõ ràng hơn cột có kiểu cụ thể.

**Cách dùng thực tế:** giữ `id`, tên, giá, stock và các trường hay lọc ở cột chuẩn. Chỉ đưa nhóm thuộc tính thật sự linh hoạt vào EAV; có metadata kiểu dữ liệu và chiến lược index. Tránh dùng EAV cho toàn bộ dữ liệu đơn hàng chỉ vì muốn “linh hoạt”.

### 4.2. JSON trong relational database

Ví dụ một cột `attributes` chứa `{"ram_gb":16,"color":"black"}`. JSON gom thông tin linh hoạt của một sản phẩm vào một dòng, đọc toàn bộ cấu hình tiện hơn EAV.

**Ưu điểm:** dễ thêm thuộc tính, hợp cấu trúc lồng nhau, giảm số bảng cho dữ liệu phụ.

**Đánh đổi:** ràng buộc kiểu và quan hệ giữa các bảng ít trực tiếp hơn; sửa tài liệu lớn và lọc nhiều JSON path có thể tốn chi phí. Có kiểu JSON không có nghĩa mọi truy vấn JSON đều được đánh index tự động.

**[Đề xuất]** Với MySQL có thể đưa JSON path hay lọc ra generated column rồi index, tùy expression và version hỗ trợ. PostgreSQL có `jsonb` cùng GIN cho nhiều bài toán containment; không đem cú pháp đó áp nguyên sang MySQL. [Nguồn: MySQL JSON data type và indexing](https://dev.mysql.com/doc/refman/8.4/en/json.html).

Ví dụ lựa chọn: RAM, CPU, cổng kết nối ít ổn định có thể là JSON; giá, SKU và tồn kho nên là cột có kiểu và constraint rõ ràng. Không lưu danh sách `order_items` trong JSON nếu nghiệp vụ cần cập nhật từng dòng và bảo toàn quan hệ chặt chẽ.

### 4.3. Adjacency List — lưu `parent_id`

Mỗi node chỉ tham chiếu cha trực tiếp. Ví dụ danh mục Laptop có `parent_id` trỏ đến Điện tử.

**[Code hiện có]** `Category` trong cả hai project dùng pattern này. Trong microservice:

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "parent_id")
private Category parentId;
```

Tên field là `parentId` nhưng kiểu thực tế là `Category`, tức là association đến entity cha, không phải một chuỗi ID.

**Ưu điểm:** đơn giản; thêm node rẻ; đổi cha thường chỉ cập nhật một dòng. Lấy con trực tiếp bằng `WHERE parent_id = ?` rất tự nhiên.

**Đánh đổi:** lấy toàn bộ cây con hoặc đường đi lên gốc cần recursive CTE hoặc nhiều lượt truy vấn; phải chặn chu trình như A là con B rồi B lại là con A. Foreign key riêng lẻ không chặn được mọi chu trình.

**Use case:** menu, category có độ sâu vừa phải và thao tác đổi cha tương đối thường xuyên. Cân nhắc index trên `parent_id`; cần kiểm tra schema thật vì MySQL có thể đã tạo index phục vụ foreign key.

### 4.4. Materialized Path — lưu đường dẫn

Mỗi node lưu đường dẫn, ví dụ `/electronics/laptop/gaming/` hoặc tốt hơn là các ID ổn định `/10/20/30/`. Lấy cây con bằng prefix của đường dẫn, với quy ước dấu phân cách để không nhầm ID `1` với `10`.

**Ưu điểm:** đọc cây con và biết đường đi thuận tiện. Prefix search có thể tận dụng B-tree nếu dạng dữ liệu, collation và điều kiện phù hợp.

**Đánh đổi:** đổi cha hoặc đổi thành phần đường dẫn phải cập nhật toàn bộ node hậu duệ; đường dẫn dài tăng dung lượng; phải giữ path nhất quán. Tìm tổ tiên bằng điều kiện đảo chiều không tự được tối ưu như tìm prefix cây con.

**Use case:** cây đọc nhiều, ít di chuyển nhánh, như cây tài liệu. Dùng ID ổn định trong path giúp đổi tên hiển thị không phải viết lại toàn bộ path. [Ví dụ chính thức về Materialized Path từ SQLAlchemy](https://docs.sqlalchemy.org/en/20/_modules/examples/materialized_paths/materialized_paths.html).

### 4.5. Closure Table — lưu mọi quan hệ tổ tiên/hậu duệ

Ngoài `categories`, thêm bảng `category_closure(ancestor_id, descendant_id, depth)`. Với A là cha B, B là cha C, lưu `(A,A,0)`, `(B,B,0)`, `(C,C,0)`, `(A,B,1)`, `(B,C,1)`, `(A,C,2)`.

**Ưu điểm:** lấy tổ tiên, hậu duệ và kiểm tra quan hệ bằng join trên bảng quan hệ; dễ biểu diễn độ sâu. Khóa/index thường bắt đầu từ `(ancestor_id, descendant_id)` và bổ sung hướng ngược khi cần.

**Đánh đổi:** ghi nhiều dòng khi thêm/di chuyển nhánh; phải cập nhật bảng closure cùng transaction. Dung lượng phụ thuộc hình dạng cây, trường hợp cây thành chuỗi có thể tăng đến bậc `n²`.

**Use case:** cây có nhiều truy vấn tổ tiên/hậu duệ hoặc kiểm tra quyền kế thừa; chấp nhận logic ghi phức tạp để đọc thuận tiện.

### 4.6. Nested Set — biết thêm khi được hỏi

Lưu hai mốc `left`, `right`; hậu duệ nằm trong khoảng của tổ tiên. Đọc cây con nhanh, nhưng chèn và di chuyển thường phải cập nhật nhiều mốc. Hợp cây khá tĩnh; ít phù hợp nếu chỉnh sửa cấu trúc thường xuyên.

### Nói khi phỏng vấn

> “Project của em dùng Adjacency List vì đơn giản và dễ cập nhật cha. Nếu cần đọc toàn bộ cây thường xuyên, em cân nhắc recursive CTE trước, rồi Closure Table hoặc Materialized Path tùy tỷ lệ đọc/ghi. EAV và JSON giải quyết thuộc tính linh hoạt, còn các trường cốt lõi như giá và tồn kho em vẫn ưu tiên cột có kiểu rõ ràng.”

## 5. Index: bản chất, loại và tradeoff

### 5.1. Index giúp gì?

Index là cấu trúc dữ liệu bổ sung để tìm vị trí dữ liệu, hỗ trợ lọc, join, sắp xếp hoặc một số phép tổng hợp. Tưởng tượng mục lục giúp tìm đến phần cần đọc thay vì đọc cả sách.

Index không bảo đảm được chọn. Optimizer còn xét số dòng khớp, độ lớn bảng, thống kê và chi phí đọc dữ liệu. Nếu truy vấn lấy phần lớn bảng, đọc tuần tự có thể rẻ hơn đi qua index rồi quay về lấy từng dòng.

### 5.2. Clustered index và secondary/non-clustered index

Với **MySQL InnoDB**, clustered index tổ chức dữ liệu dòng ở các trang lá của cây khóa chính. Nếu có primary key thì thường dùng primary key; nếu không, InnoDB có quy tắc chọn khóa thay thế hoặc tạo khóa nội bộ. Mỗi bảng chỉ có một cách tổ chức clustered này.

Secondary index lưu khóa index và primary key để tìm dòng ở clustered index. Ví dụ tìm theo tên có thể đi qua index tên rồi tra primary key để lấy các cột còn thiếu. Vì vậy primary key rộng làm nhiều secondary index rộng hơn. Đây là mô hình InnoDB, không phải mô tả áp dụng nguyên vẹn cho mọi DBMS. [Nguồn: MySQL clustered và secondary indexes](https://dev.mysql.com/doc/refman/8.4/en/innodb-index-types.html).

“Clustered” là tổ chức logic của cây và trang dữ liệu, không phải cam kết mọi byte nằm liên tục trên ổ đĩa. Query vẫn cần `ORDER BY` nếu muốn thứ tự kết quả được bảo đảm.

**[Code hiện có]** `Product.id` và `Order.id` dùng `GenerationType.UUID` với kiểu `String`. Đây là lựa chọn thuận tiện tạo ID phân tán. **[Nhận xét thiết kế]** Nếu UUID được tạo ngẫu nhiên và lưu dạng chuỗi làm PK InnoDB, khóa rộng và insert phân tán có thể làm tăng chi phí index/page split so với khóa nhỏ tăng tuần tự. Chưa đo nên không kết luận đây là bottleneck của project.

### 5.3. B-tree/B+tree phù hợp toán tử nào?

Các index thông thường của InnoDB dùng cấu trúc họ B-tree, thường được giải thích dưới dạng B+tree: khóa có thứ tự nên vừa định vị được giá trị vừa đi qua các giá trị liền kề.

- `=` và `IN`: thường phù hợp tìm đúng khóa hoặc nhiều khóa.
- `<`, `<=`, `>`, `>=`, `BETWEEN`: phù hợp range scan.
- `ORDER BY`, `MIN`, `MAX`: có thể hưởng lợi khi thứ tự index và điều kiện khớp.
- `LIKE 'lap%'`: có thể dùng khoảng prefix, tùy collation và dạng truy vấn.
- `LIKE '%lap%'`: không có điểm đầu rõ ràng để seek bằng B-tree thông thường; vẫn có thể thấy full index scan.
- `<>`, `NOT IN`: không mặc định vô dụng với index, nhưng nếu khớp phần lớn dữ liệu thì lợi ích thường thấp.

Ví dụ `WHERE DATE(created_date) = ...` có thể không tận dụng tốt index thường trên `created_date`. Có thể đổi thành khoảng `[đầu ngày, đầu ngày kế tiếp)` hoặc cân nhắc functional index nếu hệ quản trị hỗ trợ. Các phép ép kiểu và collation không tương thích cũng cần kiểm tra. [Nguồn: MySQL B-tree và hash](https://dev.mysql.com/doc/refman/8.4/en/index-btree-hash.html).

### 5.4. Hash index phù hợp toán tử nào?

Hash index băm khóa thành vị trí tra cứu, phù hợp bài toán equality `=`. Một điều kiện `IN` có thể được xử lý như nhiều phép equality lookup nếu optimizer hỗ trợ.

Hash không giữ thứ tự khóa, nên không phù hợp range `<`, `>`, `BETWEEN`, prefix `LIKE`, hoặc cung cấp thứ tự `ORDER BY`. Đừng nói “hash luôn nhanh hơn B-tree”: còn collision, cache, kích thước dữ liệu và workload.

**Lưu ý engine:** không tùy ý tạo user-defined hash index cho bảng InnoDB như cách tạo B-tree index. Adaptive Hash Index là cơ chế nội bộ khác. Vì project dùng MySQL driver, cần xác nhận engine trước khi bàn cú pháp và loại index khả dụng. [Nguồn: InnoDB Adaptive Hash Index](https://dev.mysql.com/doc/refman/8.4/en/innodb-adaptive-hash.html).

### 5.5. Full-text index

Full-text index phục vụ tìm văn bản theo token/từ, thường dựa trên ý tưởng inverted index: từ nào xuất hiện trong tài liệu nào. Nó hỗ trợ tìm kiếm ngôn ngữ và relevance tốt hơn một B-tree trên toàn bộ chuỗi.

**[Minh họa MySQL]**

```sql
CREATE FULLTEXT INDEX ft_products_name ON products(name);

SELECT id, name
FROM products
WHERE MATCH(name) AGAINST ('laptop gaming' IN NATURAL LANGUAGE MODE);
```

Full-text dùng toán tử/cú pháp riêng như `MATCH ... AGAINST`; không tự tăng tốc mọi câu `LIKE '%abc%'`. Kết quả phụ thuộc tokenizer, stopword, độ dài token và cấu hình ngôn ngữ. Cần thử dữ liệu tiếng Việt thực tế; tìm token cũng không đồng nghĩa tìm mọi chuỗi con tùy ý. [Nguồn: MySQL full-text search](https://dev.mysql.com/doc/refman/8.4/en/fulltext-search.html).

**[Code hiện có]** Shop có `LOWER(...) LIKE '%keyword%'` trong `OrderRepository.findForExport()`. B-tree thông thường khó seek cho mẫu này. Full-text có thể là lựa chọn cho tìm nội dung, nhưng mã vận đơn/số điện thoại có thể cần exact hoặc prefix search để đúng trải nghiệm; không thay tất cả bằng full-text một cách máy móc.

### 5.6. Composite index: thứ tự cột rất quan trọng

**[Minh họa theo schema microservice]**

```sql
CREATE INDEX idx_product_category_deleted_created_id
ON products(category_id, is_deleted, created_date, id);

SELECT id, name, created_date
FROM products
WHERE category_id = :categoryId
  AND is_deleted = false
ORDER BY created_date DESC, id DESC
LIMIT 20;
```

Hai cột đầu được cố định bằng equality; hai cột sau hỗ trợ thứ tự duyệt. Index tăng dần có thể được đọc ngược cho thứ tự cùng chiều `DESC, DESC`.

Có thể hiểu thứ tự khóa như sổ danh bạ sắp theo tỉnh, quận, tên. Biết tỉnh và quận thì tìm nhanh nhóm tên; chỉ biết tên mà không biết tỉnh thì index này ít thuận lợi hơn.

**Leftmost prefix:** index `(a,b,c)` thường thuận lợi cho `(a)`, `(a,b)`, `(a,b,c)`. Chỉ lọc `b` không có lợi thế seek tương tự. Một số engine/version có skip scan nên tránh phát biểu “không có a thì tuyệt đối không dùng index”.

**Sau điều kiện range:** với `(category_id, price, created_date)`, nếu lọc `category_id = ?` và `price > ?`, dữ liệu không còn được sắp toàn cục theo `created_date` trong toàn bộ khoảng giá. Có thể vẫn phải sort. Cột phía sau range vẫn có thể phục vụ lọc/coverage; không có nghĩa chúng luôn vô dụng.

Thứ tự biểu thức trong `WHERE` không quyết định thứ tự index. Viết `b = ? AND a = ?` vẫn có thể dùng `(a,b)`. Tránh quy tắc cứng “đặt cột phân biệt cao nhất trước”: cần xét equality, range, sort, join và khả năng tái sử dụng index cho các query khác.

### 5.7. Covering index

Covering index là index chứa đủ dữ liệu mà một query cần để lọc và trả kết quả, giúp giảm việc tra về dữ liệu bảng. Đây là tính chất **đối với một query cụ thể**, không phải mọi query dùng index đó đều được cover.

**[Minh họa MySQL/InnoDB]**

```sql
CREATE INDEX idx_products_category_price ON products(category_id, price);

SELECT id, price
FROM products
WHERE category_id = :categoryId;
```

Secondary index InnoDB còn chứa primary key `id`, nên ví dụ này có thể được cover. Thêm `name` vào `SELECT` thì index trên không còn chứa đủ các cột cần trả.

Trong `EXPLAIN` MySQL, `Using index` thường là dấu hiệu covering; `Using index condition` mô tả Index Condition Pushdown và không đồng nghĩa covering. Không cần nhét mọi cột vào index vì index sẽ rất rộng.

PostgreSQL có `INCLUDE` cho cột payload. Ngay cả index đủ cột, index-only scan có thể vẫn phải kiểm tra visibility ở heap khi visibility map chưa đủ điều kiện. [Nguồn: PostgreSQL index-only scans](https://www.postgresql.org/docs/17/indexes-index-only-scans.html).

### 5.8. Tradeoff khi đánh index

- **Tăng tốc đọc có chọn lọc**, join và sort phù hợp, nhưng không cứu được mọi query.
- **Chậm ghi hơn:** insert/delete phải bảo trì index; update cột có index cũng phát sinh bảo trì.
- **Tốn dung lượng và bộ nhớ cache**; index rộng làm giảm số entry trên mỗi page.
- **Tăng chi phí vận hành:** build index trên bảng lớn tiêu tốn tài nguyên; mức khóa và hỗ trợ online phụ thuộc DB/version.
- **Có thể tăng cạnh tranh khi ghi** và page split; unique index còn phải kiểm tra trùng.

**[Đề xuất cho project]** Thu SQL thật trước, nhóm theo tần suất và chi phí, đề xuất index theo query, kiểm tra index đã tồn tại rồi so sánh plan. Không chỉ đọc entity và thêm index vào mọi cột. Chưa thấy annotation `@Index` không đồng nghĩa DB không có index: PK, unique, FK hoặc migration có thể đã tạo chúng.

### Nói khi phỏng vấn

> “Index giúp giảm phạm vi đọc hoặc tránh sort nhưng phải trả chi phí ghi và dung lượng. Với InnoDB, dữ liệu nằm ở clustered primary index, secondary index tham chiếu lại bằng primary key. Em thiết kế composite index theo điều kiện equality, range và ORDER BY; sau đó dùng EXPLAIN để xác nhận. Covering index phụ thuộc các cột mà query cụ thể cần.”

## 6. Phân tích SQL chậm bằng execution plan

### 6.1. Phân biệt chậm ở ứng dụng với chậm ở SQL

Một API chậm có thể do chờ connection pool, N+1, DB chờ lock, query đọc nhiều dòng, HTTP downstream hoặc serialize JSON. Nếu chỉ tối ưu một câu SQL trong khi request phát ra 200 query thì chưa giải quyết đúng vấn đề.

Quy trình nên làm:

1. Xác định endpoint, thời điểm và độ trễ; xem tracing/log để tách thời gian DB, pool, network.
2. Lấy SQL thực tế, bind parameter, số lần chạy, số dòng trả về và kích thước dữ liệu.
3. Chạy `EXPLAIN`; nếu môi trường phù hợp thì dùng `EXPLAIN ANALYZE` cho SELECT cần đo. `ANALYZE` thực sự chạy truy vấn nên phải hiểu chi phí trước khi chạy trên dữ liệu lớn.
4. Đọc node tốn thời gian, số dòng, số vòng lặp, access path, sort và join.
5. Kiểm tra index, selectivity, thống kê, lock wait và truy vấn không sargable.
6. Sửa đúng nguyên nhân, đo lại với cùng điều kiện và kiểm tra cả tác động lên ghi.

Không đưa dữ liệu cá nhân hoặc bind parameter nhạy cảm vào log production một cách vô kiểm soát.

### 6.2. Table Scan, Index Scan và Index Seek

**Table Scan/Sequential Scan:** đọc nhiều hoặc toàn bộ dòng của bảng để kiểm tra điều kiện. Hợp lý khi bảng nhỏ hoặc query cần phần lớn bảng.

**Full Index Scan:** quét nhiều hoặc toàn bộ index. Có thể rẻ hơn table scan nếu index nhỏ hoặc cover query, nhưng không phải “đã dùng index là rất nhanh”.

**Index Seek/Range Scan:** đi tới vùng khóa phù hợp rồi đọc tập con. Tên node khác nhau giữa SQL Server, PostgreSQL và MySQL; đừng ghép thuật ngữ của một DB vào output DB khác.

### 6.3. Đọc MySQL EXPLAIN

Các giá trị hay gặp của cột `type`:

- `ALL`: full table scan.
- `index`: full index scan; không có nghĩa tra đúng một khóa.
- `range`: đọc một hoặc nhiều khoảng của index.
- `ref`: tìm các dòng theo giá trị khóa index không unique.
- `eq_ref`: thường tối đa một dòng khớp cho mỗi dòng phía trước trong join, dựa trên unique/primary key đầy đủ.
- `const`: bảng được giải quyết thành tối đa một dòng bằng điều kiện khóa phù hợp.

Đọc thêm `key` là index được chọn, `possible_keys` là ứng viên, `rows` là ước lượng dòng cần xét, `filtered` là ước lượng tỷ lệ còn lại, `Extra` là thông tin bổ sung. `Using filesort` cho biết cần bước sắp xếp; không đồng nghĩa chắc chắn ghi sort xuống đĩa. `Using temporary` cần xét kích thước dữ liệu và chi phí, không phải thấy là kết luận lỗi. [Nguồn: MySQL EXPLAIN output](https://dev.mysql.com/doc/refman/8.4/en/explain-output.html).

`EXPLAIN` thường là ước lượng. Với `EXPLAIN ANALYZE`, so actual rows/time/loops với ước lượng. Nếu node rất rẻ nhưng lặp hàng trăm nghìn lần thì tổng chi phí vẫn lớn. Không đọc cost như số mili giây.

### 6.4. Bài tập theo project

**[Minh họa; chưa có plan đo thật]**

```sql
EXPLAIN
SELECT id, name, price
FROM products
WHERE is_deleted = false
ORDER BY created_date DESC, id DESC
LIMIT 20 OFFSET 100000;
```

Điểm cần trả lời: có index hỗ trợ filter và sort chưa; phải bỏ qua bao nhiêu entry; có tra bảng để lấy `name`, `price` không; count query của `Page` tốn bao nhiêu? Sau đó thử keyset ở phần 9. Một index đơn trên boolean `is_deleted` thường có selectivity thấp nếu hầu hết dòng chưa bị xóa; index ghép vẫn có thể hữu ích tùy query.

### Nói khi phỏng vấn

> “Em lấy SQL và parameter thật, đọc EXPLAIN để xem access path, số dòng và sort, rồi dùng actual plan khi cần đo. Em không coi Table Scan luôn xấu hoặc Index Scan luôn tốt. Với API, em kiểm tra thêm N+1, lock wait và connection pool để xác định đúng chỗ chậm.”

## 7. Transaction isolation

### 7.1. Isolation khác propagation

**Isolation** hỏi: “Các transaction đồng thời được nhìn thấy dữ liệu của nhau ra sao?” Đây là khái niệm của DB.

**Propagation** hỏi: “Một method được gọi khi đã có transaction thì tham gia, tạo mới hay chạy ngoài transaction?” Đây là chính sách quản lý transaction ở framework, sẽ học tại phần 16.

### 7.2. Những hiện tượng cần hiểu

- **Dirty read:** T1 đọc thay đổi chưa commit của T2; T2 rollback thì dữ liệu T1 đã dùng chưa từng trở thành dữ liệu đã commit.
- **Non-repeatable read:** T1 đọc cùng một dòng hai lần và thấy giá trị khác vì T2 đã update rồi commit ở giữa.
- **Phantom read:** T1 chạy lại điều kiện tìm kiếm và tập dòng phù hợp thay đổi vì transaction khác thêm/xóa/sửa dữ liệu.
- **Lost update:** hai bên cùng đọc stock 10; mỗi bên trừ 1 rồi cùng ghi 9, làm mất một lần trừ.
- **Write skew:** hai bên dựa trên cùng một trạng thái tổng thể rồi sửa các dòng khác nhau, khiến một quy tắc liên dòng bị vi phạm. Snapshot nhất quán chưa chắc ngăn được tình huống này.

Ba hiện tượng đầu thường dùng để giới thiệu mức isolation. Hai hiện tượng sau nhắc rằng “đọc lặp lại được” chưa đủ để kết luận nghiệp vụ đồng thời an toàn.

### 7.3. Bốn mức phổ biến

**READ UNCOMMITTED:** theo mô hình SQL, có thể đọc dữ liệu chưa commit. Isolation yếu, ít phù hợp nghiệp vụ cần chính xác. Không mặc định là lựa chọn tối ưu chỉ vì nghe có vẻ ít lock.

**READ COMMITTED:** chỉ đọc dữ liệu đã commit; vẫn có thể có non-repeatable read và phantom. Thường mỗi statement có snapshot mới nên hai câu SELECT trong cùng transaction có thể thấy kết quả khác nhau. Thường là điểm cân bằng tốt nếu kết hợp khóa/conditional update đúng nghiệp vụ.

**REPEATABLE READ:** ngăn dirty read và non-repeatable read theo định nghĩa mức chuẩn, nhưng mức chuẩn này vẫn cho phép phantom; hành vi cụ thể phụ thuộc DBMS và loại truy vấn. Trong InnoDB, consistent nonlocking reads thường dùng snapshot được thiết lập ở lần consistent read đầu tiên. Locking read/DML có hành vi khác và có thể dùng next-key/gap lock để bảo vệ phạm vi.

**SERIALIZABLE:** nhằm cho hiệu ứng tương đương một thứ tự thực thi tuần tự của các transaction đã commit. Có thể tăng chờ hoặc phát sinh lỗi cần retry; cơ chế thực hiện tùy DB. Không có nghĩa DB bắt buộc chỉ chạy đúng một transaction tại mọi thời điểm.

Với Spring còn có **`Isolation.DEFAULT`**, nghĩa là sử dụng mức mặc định của hệ thống transaction/DB, không phải một mức SQL mới. [Nguồn: Spring Isolation API](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/transaction/annotation/Isolation.html).

MySQL InnoDB mặc định là REPEATABLE READ nếu không bị cấu hình lại. Tuy vậy không suy ra deployment của project đang dùng mức này khi chưa kiểm tra cấu hình/session. [Nguồn: MySQL transaction isolation](https://dev.mysql.com/doc/refman/8.4/en/innodb-transaction-isolation-levels.html).

**Khác biệt cần nhớ:** PostgreSQL xử lý READ UNCOMMITTED như READ COMMITTED; REPEATABLE READ của PostgreSQL tránh phantom ở snapshot nhưng vẫn có thể gặp serialization anomaly. Cùng tên mức isolation không bảo đảm hai DB có mọi chi tiết giống nhau. [Nguồn: PostgreSQL isolation](https://www.postgresql.org/docs/current/transaction-iso.html).

### 7.4. Chọn mức isolation như thế nào?

Không tăng lên SERIALIZABLE cho toàn bộ ứng dụng chỉ để “cho chắc”. Xác định invariant trước: tồn kho không âm, coupon không vượt hạn mức, hay dữ liệu báo cáo chỉ cần gần đúng? Sau đó chọn isolation kết hợp optimistic lock, pessimistic lock, unique constraint hoặc conditional update.

Với hàng tồn kho, tăng isolation không thay thế việc kiểm tra điều kiện và xử lý xung đột. Read-modify-write ở Java cần bảo vệ rõ ràng.

### Nói khi phỏng vấn

> “Isolation điều khiển tương tác giữa các transaction đồng thời. Em hiểu dirty read, non-repeatable read và phantom, nhưng khi thiết kế tồn kho em còn xét lost update. Em không dựa riêng vào isolation mặc định; em chọn thêm lock hoặc update có điều kiện dựa trên invariant nghiệp vụ và mức tranh chấp.”

## 8. Pessimistic lock và optimistic lock

Tên đúng là **pessimistic**, không phải `permistic`. Hai cách đều để phối hợp cập nhật đồng thời nhưng xử lý xung đột ở thời điểm khác nhau.

### 8.1. Pessimistic lock — khóa trước khi sửa

Ý tưởng: dự đoán có tranh chấp nên giữ khóa DB cho dòng cần sửa. Ví dụ `SELECT ... FOR UPDATE`, kiểm tra stock rồi trừ trong cùng transaction. Khóa ghi thường được giữ đến commit/rollback.

Các transaction muốn ghi cạnh tranh có thể phải chờ; SELECT thường theo MVCC không nhất thiết bị chặn giống ghi. Phạm vi khóa phụ thuộc isolation, index và query plan, không phải cứ nhìn `WHERE id = ...` là hiểu mọi loại lock.

**[Code hiện có]** [CouponRepository.java](../ecomerc%20shop/ecommerc_shop_backend/src/main/java/org/example/ecommerc_shop/repository/CouponRepository.java), dòng 28:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("""
    SELECT c FROM Coupon c
    WHERE c.code = :code
      AND c.deleted = false
      AND c.status = 'ACTIVE'
      AND :now BETWEEN c.startDate AND c.endDate
      AND (c.usageLimit IS NULL OR c.usedCount < c.usageLimit)
""")
Optional<Coupon> findValidCoupon(
        @Param("code") String code,
        @Param("now") LocalDateTime now);
```

`OrderTransactionService` gọi query này rồi tăng `usedCount` trong transaction. Mục tiêu là tránh nhiều đơn cùng vượt lượt coupon. Cần mọi luồng cập nhật cùng tuân thủ quy tắc; đồng thời kiểm tra index/unique cho `code` để truy vấn và phạm vi khóa hợp lý.

**Ưu điểm:** thích hợp vùng tranh chấp cao hoặc tài nguyên khan hiếm. **Đánh đổi:** chờ lock, deadlock, giảm throughput nếu transaction dài. Giữ transaction ngắn, truy cập tài nguyên theo thứ tự nhất quán và có chính sách retry phù hợp cho lỗi transient.

### 8.2. Optimistic lock — kiểm tra version lúc ghi

Ý tưởng: cho phép đọc/sửa song song, nhưng khi ghi phải chứng minh dữ liệu vẫn ở version đã đọc.

**[Code hiện có]** [ProductVariant.java](../ecomerc%20shop/ecommerc_shop_backend/src/main/java/org/example/ecommerc_shop/entity/ProductVariant.java), dòng 45:

```java
@Version
@Column(nullable = false)
private Long version;
```

**[Minh họa SQL do ORM có thể sinh]**

```sql
UPDATE product_variants
SET quantity_in_stock = :newStock,
    version = :oldVersion + 1
WHERE id = :id
  AND version = :oldVersion;
```

T1 và T2 cùng đọc version 5. T1 update thành version 6 trước; T2 không update được dòng version 5 nữa, ORM báo optimistic locking conflict. Lỗi có thể xuất hiện lúc flush/commit, không nhất thiết tại dòng setter. [Nguồn: Jakarta Persistence Version](https://jakarta.ee/specifications/persistence/3.2/apidocs/jakarta.persistence/jakarta/persistence/Version.html).

**Ưu điểm:** không giữ pessimistic lock suốt thời gian đọc và xử lý; hợp khi ít xung đột. **Đánh đổi:** xung đột dẫn đến rollback/retry hoặc báo người dùng tải lại. “Optimistic không dùng lock” là cách nói quá đơn giản: DB vẫn có cơ chế khóa khi thực thi UPDATE.

Bulk JPQL/native SQL update có thể đi ngoài cơ chế version tự động; phải chủ động bảo toàn version và refresh/clear persistence context khi cần.

**[Code hiện có]** `OrderTransactionService.createOrderTransaction()` có `@Retryable`, `maxAttempts = 3`, backoff 100 ms cho một số optimistic locking exception; ứng dụng có `@EnableRetry`. `OrderServiceImpl.createOrder()` gọi sang bean này nên có đường đi qua proxy.

**Điểm cần kiểm chứng thêm:** retry advice phải bao bên ngoài transaction advice để mỗi lần thử có transaction mới; exception thực tế phải khớp cấu hình. Source thể hiện ý định, chưa thay concurrency test chứng minh retry vận hành đúng. Không retry mù quáng trong transaction đã lỗi hoặc phát sinh lại các side effect ngoài DB.

### 8.3. Atomic conditional update — một lựa chọn khác

**[Đề xuất cho tồn kho microservice]**

```sql
UPDATE products
SET stock = stock - :quantity
WHERE id = :id
  AND is_deleted = false
  AND :quantity > 0
  AND stock >= :quantity;
```

Kiểm tra affected rows: 1 nghĩa là đã trừ; 0 nghĩa là không đủ điều kiện hoặc không có sản phẩm. Phép kiểm tra và trừ nằm trong một statement, tránh khoảng hở giữa đọc rồi ghi ở Java. Nếu trừ nhiều sản phẩm cùng đơn, vẫn cần transaction và rollback khi một sản phẩm không đáp ứng.

Lệnh này chưa giải quyết message trùng: cùng event được xử lý hai lần vẫn có thể trừ hai lần. Phải bổ sung idempotency. Nếu áp dụng cho entity có `@Version`, cũng phải thiết kế cách cập nhật version để không phá optimistic locking.

### 8.4. Dẫn chứng microservice: có khai báo lock chưa chắc đang dùng

**[Code hiện có]** `ProductRepository.findByIdForUpdate()` có `PESSIMISTIC_WRITE`, nhưng `ProductServiceImpl.updateStock()` hiện gọi `findByIdIn(sortedIds)`, không gọi method lock đó.

`updateStock()` dùng Redisson lock với khóa tạo từ **cả danh sách ID**, ví dụ `lock:products:A,B`. Request mua `[A,B]` và request mua `[B,C]` sẽ có hai khóa khác nhau dù cùng sửa B. Vì thế cách đặt khóa này chưa bảo vệ tài nguyên trùng nhau giữa hai tập khác nhau.

Method còn `tryLock(10, 5, SECONDS)` với lease 5 giây và `Thread.sleep(4000)`. Có rủi ro lease hết trước khi xong công việc/commit. `unlock()` nằm trong `finally` của method; với transaction proxy thông thường, commit xảy ra sau khi method target trả về, nên có thể nhả Redis lock trước DB commit. Khi đọc được products, code trừ trực tiếp mà chưa kiểm tra `stock >= quantity` tại điểm cập nhật đó.

**[Đề xuất]** Cân nhắc conditional update, hoặc pessimistic lock trong DB transaction; validate số lượng dương, gộp ID trùng và kiểm tra đủ tất cả sản phẩm. Nếu vẫn dùng distributed lock thì khóa theo tài nguyên, giữ thứ tự lấy khóa và thiết kế ranh giới đến commit; không coi Redis lock là thay thế cho tính đúng đắn của DB.

### Nói khi phỏng vấn

> “Shop dùng optimistic lock qua `@Version` cho tồn kho và pessimistic lock cho coupon có giới hạn lượt. Optimistic phát hiện conflict lúc ghi nên phải có cách retry hoặc phản hồi; pessimistic khóa sớm nên cần transaction ngắn. Với microservice hiện tại, em thấy lock theo cả tập ID chưa bảo vệ được các tập giao nhau, nên hướng cải thiện là cập nhật stock có điều kiện hoặc khóa DB đúng transaction.”

## 9. Pagination

### 9.1. Offset/Limit

```sql
SELECT id, name
FROM products
WHERE is_deleted = false
ORDER BY created_date DESC, id DESC
LIMIT 20 OFFSET 100000;
```

Database thường vẫn phải đi qua vùng offset rồi bỏ các dòng trước đó. Index giúp thứ tự và tìm dữ liệu, nhưng không khiến việc bỏ qua 100.000 entry tự nhiên miễn phí.

**Ưu điểm:** dễ code, dễ nhảy đến trang bất kỳ, hợp màn hình admin có số trang. **Đánh đổi:** trang sâu đắt hơn; dữ liệu thêm/xóa giữa các request có thể làm trùng hoặc bỏ sót dòng.

**[Code hiện có]** `ProductController.getAllProducts()` dùng `PageRequest.of(pageNumber, pageSize)`: trang bắt đầu từ 0. Code hiện chưa truyền `Sort`, nên **thứ tự trả về chưa được bảo đảm**. Đây là vấn đề cần sửa trước khi bàn tối ưu pagination sâu.

Shop thường dùng `PageRequest.of(pageNumber - 1, pageSize, Sort.by("createdAt").descending())`, tức API nhận trang bắt đầu từ 1 rồi đổi sang 0. Nên thêm `id` làm tie-breaker vì nhiều dòng có thể có cùng thời điểm tạo.

### 9.2. Keyset/Cursor

Thay vì “bỏ qua bao nhiêu dòng”, hỏi “lấy các dòng nằm sau khóa cuối của trang trước”.

**[Minh họa ID tăng dần]**

```sql
SELECT id, name
FROM products
WHERE is_deleted = false
  AND id > :lastId
ORDER BY id ASC
LIMIT 20;
```

Điều kiện cursor phải khớp thứ tự sort. Trong project ID là UUID, `id > lastId` biểu diễn thứ tự so sánh ID theo DB, **không biểu diễn thời gian tạo**. Nếu UI muốn mới nhất trước thì dùng thời gian kèm ID.

**[Đề xuất cho microservice, sắp mới nhất trước]**

```sql
SELECT id, name, created_date
FROM products
WHERE is_deleted = false
  AND (
      created_date < :lastCreatedDate
      OR (created_date = :lastCreatedDate AND id < :lastId)
  )
ORDER BY created_date DESC, id DESC
LIMIT 21;
```

Lấy 21 dòng cho page size 20 để biết còn trang sau; trả 20 dòng và tạo cursor từ dòng thứ 20. Trang đầu bỏ điều kiện cursor. Một index ứng viên là `(is_deleted, created_date, id)`; nếu lọc thêm category thì thiết kế lại theo query.

`created_date` nên không null và không đổi sau khi tạo; cursor cần giữ đủ độ chính xác timestamp. Để so sánh nhất quán, dùng cùng collation/thứ tự như DB. Nếu người dùng đổi filter hoặc sort thì cursor cũ không còn hợp lệ.

**Ưu điểm:** với index phù hợp, chi phí thường phụ thuộc số dòng lấy tiếp thay vì độ sâu offset. **Đánh đổi:** khó nhảy ngay đến trang 100, khó tính số trang; query filter ít chọn lọc vẫn có thể phải đi qua nhiều entry.

Cursor không tự giữ snapshot xuyên nhiều HTTP request: dòng bị xóa hoặc khóa sort bị sửa vẫn có thể ảnh hưởng kết quả. Nếu cần export tập dữ liệu cố định, phải thiết kế snapshot hoặc mốc giới hạn phù hợp.

### 9.3. Page và Slice

`Page` thường cần thêm count query để trả tổng số phần tử/trang; có trường hợp framework tối ưu bỏ count. `Slice` chủ yếu trả nội dung và có trang kế tiếp hay không, thường lấy thêm một dòng.

**[Code hiện có]** Shop dùng `Slice<Order>` cho export. Đây là cách tránh yêu cầu total count; **Slice dùng với PageRequest vẫn có thể là offset pagination**, chưa tự biến thành keyset.

### Nói khi phỏng vấn

> “Offset phù hợp danh sách cần nhảy trang, nhưng trang sâu phải bỏ qua nhiều dòng. Keyset dùng khóa cuối trang trước với ORDER BY và index tương ứng nên ổn định hơn theo độ sâu. Em dùng thêm ID để phá hòa timestamp. Trong project, PageRequest hiện là offset; Slice chỉ giúp bỏ total count, không tự chuyển thành cursor.”

## 10. Partitioning và sharding

### 10.1. Partitioning

Theo cách phân biệt thường dùng khi phỏng vấn, partitioning chia một bảng logic thành nhiều phần do cùng một DBMS quản lý. Ví dụ partition đơn hàng theo tháng, hoặc theo hash của customer ID.

**Lợi ích:** partition pruning có thể bỏ qua các phần không liên quan; dễ quản lý dữ liệu cũ, như bỏ một partition lịch sử thay vì delete từng dòng. Không có điều kiện phù hợp partition key thì DB vẫn có thể đọc nhiều partition. [Nguồn: MySQL partitioning](https://dev.mysql.com/doc/refman/8.4/en/partitioning-overview.html).

**Đánh đổi:** chọn partition key khó; nhiều partition tạo overhead; unique key và foreign key có giới hạn tùy DB/engine. Với MySQL, phải kiểm tra riêng các giới hạn của partitioned InnoDB tables trước khi áp dụng vào schema quan hệ hiện có. Partitioning không tự tăng CPU/RAM bằng cách thêm server.

### 10.2. Sharding

Sharding phân phối các phần dữ liệu sang nhiều database/node. Ví dụ định tuyến customer vào shard theo hash customer ID.

**Lợi ích:** scale dung lượng và ghi theo chiều ngang, phân tán tải nếu shard key tốt.

**Đánh đổi:** ứng dụng hoặc middleware phải biết route; join và transaction xuyên shard phức tạp; khó bảo đảm uniqueness toàn cục; cần xử lý hotspot, resharding và backup/khôi phục nhiều shard. Hash modulo đơn giản có thể phải di chuyển nhiều dữ liệu khi đổi số shard.

**Replication** lại là sao chép dữ liệu để tăng khả năng sẵn sàng hoặc scale đọc; không đồng nghĩa chia dữ liệu ghi thành các shard.

### 10.3. Liên hệ project

**[Code hiện có]** Project tách `product-service` và `order-service` theo trách nhiệm nghiệp vụ. Đây là **phân rã theo domain/service**, không tự đồng nghĩa sharding. Sharding sẽ là chia cùng tập đơn hàng ra nhiều nơi theo một shard key. Không thấy bằng chứng triển khai partition/shard routing trong các source đã đọc.

**[Đề xuất]** Nếu lịch sử đơn hàng tăng lớn, có thể đánh giá partition theo thời gian cho nhu cầu quản lý dữ liệu. Chỉ cân nhắc sharding khi đã có số liệu giới hạn của một DB, sau khi xem index, query, archive và scale phù hợp.

### Nói khi phỏng vấn

> “Partitioning thường chia một bảng thành các phần bên trong cùng hệ quản trị để pruning và quản lý dữ liệu. Sharding chia dữ liệu sang nhiều node để scale ngang, đổi lại phải xử lý routing, transaction và join xuyên shard. Tách order-service với product-service là tách domain, chưa phải sharding đơn hàng.”

## 11. N+1 query trong JPA

### 11.1. N+1 xuất hiện như thế nào?

Ứng dụng chạy một query lấy N sản phẩm, rồi lúc map response lại truy cập `product.getCategory().getName()`. Nếu mỗi association chưa được tải, ORM có thể chạy thêm query lấy category cho từng sản phẩm/nhóm category chưa có trong persistence context.

Kịch bản xấu là 1 query lấy danh sách và N query tải liên quan. Nếu có count query thì tổng còn thêm query đếm; nếu nhiều sản phẩm chung category, first-level cache có thể làm số query nhỏ hơn N. “N+1” mô tả một kiểu truy cập, không nhất thiết mọi lần chạy đều đúng N+1 câu.

### 11.2. Dẫn chứng nguy cơ trong microservice

**[Code hiện có]** Ba mảnh code kết hợp:

- `Product.category` là `@ManyToOne(fetch = LAZY)`.
- `ProductServiceImpl.getAllProducts()` gọi repository lấy `Page<Product>` rồi `.map(productMapper::toResponse)`.
- `ProductMapper.toResponse()` ánh xạ `category.name` sang `categoryName`.

Đây là **nguy cơ N+1 có cơ sở từ source**, chưa phải kết quả đếm SQL thực tế. `@Transactional(readOnly = true)` giữ phạm vi persistence context cho việc map trong service, nhưng không tự giảm query thành một câu.

### 11.3. LAZY và EAGER

**LAZY:** trì hoãn tải association; truy cập ngoài persistence context có thể gây `LazyInitializationException` nếu dữ liệu chưa được tải. Trong JPA, LAZY là hint; hành vi cụ thể còn phụ thuộc provider và mapping.

**EAGER:** yêu cầu dữ liệu liên quan được tải sớm theo quy định của ORM, nhưng **không bảo đảm luôn dùng một JOIN duy nhất**. Provider có thể chạy secondary select, vẫn tạo vấn đề nhiều query.

Theo mặc định JPA thường gặp: `ManyToOne`/`OneToOne` là EAGER; `OneToMany`/`ManyToMany` là LAZY. Không đổi tất cả sang EAGER để chữa N+1; nên chọn fetch plan theo từng use case. [Nguồn: Jakarta Persistence FetchType](https://jakarta.ee/specifications/persistence/3.2/apidocs/jakarta.persistence/jakarta/persistence/FetchType.html).

### 11.4. Các cách xử lý

**Fetch join:** dùng JPQL yêu cầu tải association cần cho use case.

**[Code hiện có]** Shop có `CartItemRepository.findCartItemsWithProductVariant()`:

```java
@Query("""
    SELECT ci FROM CartItem ci
    JOIN FETCH ci.productVariant pv
    JOIN FETCH pv.product p
    WHERE ci.cart.id = :cartId
      AND ci.deleted = false
""")
List<CartItem> findCartItemsWithProductVariant(@Param("cartId") String cartId);
```

`CartServiceImpl.getMyCart()` dùng query này để lấy dữ liệu tính giỏ hàng và map DTO. Đây là dẫn chứng bạn đã dùng fetch join cho association cần đọc.

**Entity graph:** khai báo association cần tải mà không phải viết lại toàn bộ JPQL. **[Code hiện có]** Shop có `@EntityGraph(attributePaths = {"user"})` trên `OrderRepository.findAllByDeletedFalse(Pageable)` và trên query notification. [Nguồn: Spring Data JPA entity graph](https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html#jpa.entity-graph).

**DTO projection:** chỉ select các cột thực sự cần, ví dụ tên sản phẩm và tên danh mục. Hợp API đọc, giảm tải entity và tránh vô tình đi qua association.

**Batch fetching:** tải association theo nhóm thay vì từng entity. Giảm số query nhưng không phải luôn chỉ còn đúng một câu. Cần chọn kích thước batch phù hợp.

**[Đề xuất cho microservice]** Có thể đánh graph cho quan hệ to-one `category`:

```java
@EntityGraph(attributePaths = "category")
Page<Product> findAllByIsDeletedFalse(Pageable pageable);
```

### 11.5. Bẫy fetch join collection với pagination

Fetch collection một-nhiều làm số dòng SQL tăng theo số phần tử con. `LIMIT` trên dòng join không tương đương giới hạn số entity cha; ORM có thể phân trang trong bộ nhớ hoặc từ chối tùy cấu hình.

Với case cần page đơn hàng kèm nhiều dòng hàng, cách thường dùng là lấy page ID đơn trước, query tiếp các association theo tập ID, rồi phục hồi đúng thứ tự. To-one fetch như `Product -> Category` thường không có vấn đề nhân dòng kiểu collection này. [Nguồn: Hibernate fetch và pagination](https://docs.hibernate.org/orm/7.1/userguide/html_single/#hql-join-fetch).

### 11.6. Xác nhận đã xử lý thế nào?

Trong môi trường dev/test, bật SQL log hoặc thống kê query, gọi endpoint với dữ liệu đủ đa dạng rồi đếm query. Kiểm tra cả response để bảo đảm fetch đúng association. Dùng integration test về số query nếu đây là rủi ro hiệu năng quan trọng; đừng chỉ thấy annotation là khẳng định hết N+1.

### Nói khi phỏng vấn

> “N+1 xảy ra khi lấy danh sách rồi tải quan hệ riêng từng phần tử. Trong shop em đã dùng JOIN FETCH để tải cart item, variant và product, và EntityGraph để tải user cùng đơn hàng. Em giữ LAZY làm điểm xuất phát cho association phù hợp rồi chọn fetch plan theo API; EAGER không bảo đảm một query. Với collection và pagination, em tránh fetch join trực tiếp mà lấy page ID trước.”

## 12. Trigger và CTE

### 12.1. Trigger

Trigger là logic chạy trong DB khi có thao tác như INSERT, UPDATE hoặc DELETE. Ví dụ `AFTER UPDATE` trên stock ghi lịch sử thay đổi; `BEFORE UPDATE` kiểm tra một điều kiện.

**Ưu điểm:** áp dụng cho nhiều ứng dụng cùng ghi DB, khó bị bỏ qua như một hàm validation chỉ nằm ở một API. Hữu ích cho một số audit hoặc quy tắc dữ liệu cần thực thi tập trung.

**Đánh đổi:** side effect ít hiện rõ trong service code, khó debug/test, tăng chi phí ghi và phụ thuộc DBMS. Nếu logic service và trigger cùng cập nhật một dữ liệu dẫn xuất có thể ghi hai lần hoặc gây vòng phụ thuộc.

Với InnoDB, thay đổi dữ liệu transactional do trigger thực hiện thuộc transaction của statement gọi nó; trigger không phải transaction độc lập để commit riêng. Không dùng trigger như cách “gửi Kafka chắc chắn cùng commit”; tác động ngoài DB không tự có bảo đảm nguyên tử. [Nguồn: MySQL trigger syntax](https://dev.mysql.com/doc/refman/8.4/en/trigger-syntax.html).

**[Phạm vi project]** Chưa thấy định nghĩa trigger trong source đã kiểm tra. Nếu muốn áp dụng, phải kiểm tra cả schema đang chạy vì trigger có thể được tạo ngoài repo.

### 12.2. CTE — Common Table Expression

CTE là tập kết quả có tên trong phạm vi một statement, khai báo bằng `WITH`. Giúp chia truy vấn dài thành các bước dễ đọc; không phải bảng lưu bền hoặc cache dùng chung giữa các request.

**[Minh họa theo schema microservice]**

```sql
WITH order_totals AS (
    SELECT order_id, SUM(price * quantity) AS calculated_total
    FROM order_items
    GROUP BY order_id
)
SELECT o.id, o.total_amount, t.calculated_total
FROM orders o
JOIN order_totals t ON t.order_id = o.id
WHERE o.total_amount <> t.calculated_total;
```

Ví dụ kiểm tra tổng lưu sẵn với tổng tính lại cho đơn có dòng hàng. Muốn kiểm tra cả đơn chưa có item thì phải thiết kế thêm `LEFT JOIN` và xử lý `NULL`.

**Recursive CTE** có phần neo và phần lặp; phù hợp cây danh mục Adjacency List:

```sql
WITH RECURSIVE category_tree AS (
    SELECT id, name, parent_id, 0 AS depth
    FROM categories
    WHERE id = :rootId

    UNION ALL

    SELECT c.id, c.name, c.parent_id, t.depth + 1
    FROM categories c
    JOIN category_tree t ON c.parent_id = t.id
    WHERE t.depth < :maxDepth
)
SELECT id, name, parent_id, depth
FROM category_tree;
```

Đây là ví dụ mở rộng, chưa phải query hiện có. Giới hạn độ sâu giúp chặn chạy quá dài nhưng không thay thế validation chống chu trình. Cần bổ sung chính sách lọc soft delete, thứ tự kết quả và giới hạn tài nguyên theo use case.

CTE không mặc định nhanh hơn subquery; optimizer có thể merge hoặc materialize tùy DB và truy vấn. Xem plan thay vì mặc định “WITH là tạo bảng tạm” hay “WITH chắc chắn tăng hiệu năng”. [Nguồn: MySQL WITH/CTE](https://dev.mysql.com/doc/refman/8.4/en/with.html).

### Nói khi phỏng vấn

> “Trigger là logic chạy ở database theo sự kiện ghi, hữu ích cho một số audit nhưng có side effect khó nhìn từ code. CTE giúp đặt tên cho từng bước truy vấn; recursive CTE hợp với cây parent_id trong project. Em chọn chúng vì tính phù hợp và khả năng bảo trì, còn hiệu năng phải đọc execution plan.”

## 13. IoC, DI và constructor injection

### 13.1. IoC và DI khác nhau thế nào?

**IoC — Inversion of Control:** chuyển quyền điều khiển một phần vòng đời/chạy chương trình cho framework hoặc container. Trong Spring, container quản lý tạo object, kết nối dependency và lifecycle của bean.

**DI — Dependency Injection:** một cách thực hiện IoC. Object nhận dependency từ bên ngoài thay vì tự `new` implementation cụ thể bên trong.

**[Minh họa trước DI]**

```java
class OrderService {
    private final ProductClient client = new HttpProductClient();
}
```

Service gắn chặt với cách tạo HTTP client. Muốn test bằng client giả hoặc đổi cấu hình sẽ khó hơn.

**[Minh họa constructor DI]**

```java
class OrderService {
    private final ProductClient client;

    OrderService(ProductClient client) {
        this.client = client;
    }
}
```

Spring có thể truyền client thật; unit test có thể truyền fake/mock. Service tập trung vào nghiệp vụ thay vì tự quản lý khởi tạo dependency.

Bean là object được Spring container quản lý. `new SomeService()` ở một nơi tùy ý không tự biến object đó thành Spring bean và không tự được áp dụng transaction/cache/async advice.

### 13.2. Ba cách injection

**Constructor injection:** truyền dependency qua constructor. Hợp dependency bắt buộc, có thể giữ reference bằng `final`, object bộc lộ đầy đủ những gì cần để hoạt động.

**Setter injection:** container gọi setter sau khi tạo object. Hợp một số dependency tùy chọn hoặc cấu hình có thể thay đổi. “Setter” không tự có nghĩa optional; annotation/config vẫn có thể yêu cầu bắt buộc.

**Field injection:** đánh `@Autowired` trực tiếp vào field. Viết ngắn nhưng dependency bị giấu khỏi constructor; object tự `new` có thể thiếu dependency; unit test thường phải dùng reflection hoặc khởi động Spring để inject. Không phù hợp cho immutable dependency reference như constructor với `final`.

### 13.3. Vì sao ưu tiên constructor?

Dependency bắt buộc được cung cấp ngay khi tạo; dễ đọc và test. Constructor quá nhiều tham số còn là tín hiệu class đang ôm nhiều trách nhiệm. Nếu hai class phụ thuộc vòng qua constructor, lỗi cũng hiện rõ để xem lại thiết kế thay vì giấu vòng phụ thuộc.

`final` chỉ ngăn đổi reference, không làm object bên trong tự immutable hoặc thread-safe. Constructor injection cũng không tự chống mọi giá trị null nếu bạn chủ động gọi constructor bằng null ngoài container. [Nguồn: Spring dependency injection](https://docs.spring.io/spring-framework/reference/core/beans/dependencies/factory-collaborators.html).

### 13.4. Dẫn chứng trong project

**[Code hiện có, rút gọn]** `ProductServiceImpl`:

```java
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;
    // Các dependency khác được bỏ khỏi đoạn trích.
}
```

Lombok `@RequiredArgsConstructor` sinh constructor cho các field `final` chưa khởi tạo. Với bean chỉ có một constructor phù hợp, Spring không cần bạn ghi thêm `@Autowired` lên constructor đó.

`@Mapper(componentModel = "spring")` khiến MapStruct tạo implementation mapper dạng Spring bean, để service có thể inject mapper thay vì tự tạo thủ công.

Nếu có nhiều bean cùng kiểu, có thể dùng `@Qualifier` hoặc `@Primary` để chọn rõ. DI không mặc định biết nghiệp vụ muốn implementation nào khi có nhiều ứng viên ngang nhau.

### Nói khi phỏng vấn

> “IoC là chuyển quyền quản lý object cho container, còn DI là cách cung cấp dependency từ bên ngoài. Project em dùng constructor injection qua RequiredArgsConstructor và field final. Cách này thể hiện dependency bắt buộc, dễ test bằng mock và giúp phát hiện service có quá nhiều trách nhiệm.”

## 14. Bean scope và vòng đời bean

### 14.1. Singleton và Prototype

**Singleton:** mặc định, container dùng một instance cho một bean definition trong container đó. Không phải “duy nhất một object trong toàn bộ JVM, bất kể có bao nhiêu container”. Nhiều HTTP request có thể gọi cùng service instance nên tránh giữ dữ liệu riêng request trong field mutable của service.

**Prototype:** container tạo instance mới mỗi lần có yêu cầu lấy bean prototype. Không có nghĩa mỗi lần gọi một method của reference đã giữ thì tự sinh object mới. Spring không quản lý đầy đủ destruction của prototype như singleton; bên sử dụng chịu trách nhiệm tài nguyên cần đóng. [Nguồn: Spring bean scopes](https://docs.spring.io/spring-framework/reference/core/beans/factory-scopes.html).

Các scope web khác gồm request và session; chúng có ý nghĩa trong web-aware context. Không dùng session scope như giải pháp tùy tiện cho việc chia sẻ state giữa nhiều node.

**[Code hiện có]** Các `@Service` không khai báo scope trong project dùng singleton theo mặc định. Ví dụ `ProductServiceImpl` có nhiều biến local trong method; biến local không tự trở thành dữ liệu dùng chung như field của singleton. Tuy nhiên các dependency hoặc tài nguyên ngoài vẫn có thể bị truy cập đồng thời.

### 14.2. Inject Prototype vào Singleton: điều gì thực sự xảy ra?

Nếu inject trực tiếp một prototype vào constructor của singleton, dependency được resolve khi tạo singleton. Singleton giữ **một instance prototype đã nhận** và dùng lại reference đó.

Scope của bean prototype **không bị đổi thành singleton**. Một lần gọi container lấy prototype riêng vẫn nhận instance khác. Cách nói đúng là “singleton giữ một instance prototype cố định”, không phải “prototype vô tình đổi scope”.

### 14.3. Giải pháp lấy instance mới khi cần

**[Minh họa; chưa thấy use case này trong project]**

```java
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class ExportJob {
    void run() { /* trạng thái riêng cho từng job */ }
}

@Service
class ExportService {
    private final ObjectProvider<ExportJob> jobs;

    ExportService(ObjectProvider<ExportJob> jobs) {
        this.jobs = jobs;
    }

    void export() {
        ExportJob job = jobs.getObject();
        job.run();
    }
}
```

`getObject()` là điểm yêu cầu instance mới của prototype trong ví dụ. `ObjectFactory<T>` có thể phục vụ nhu cầu tương tự; `jakarta.inject.Provider<T>` dùng `get()`. Đây là lookup có chủ đích qua provider, khác với inject sẵn một job rồi dùng mãi. [Nguồn: ObjectProvider API](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/beans/factory/ObjectProvider.html).

`@Lookup` là lựa chọn khác: Spring override method lookup để lấy bean từ container mỗi lần gọi. Cơ chế dựa trên subclass nên có giới hạn như class/method không được final và không áp dụng tùy ý cho object tự `new` hoặc mọi factory method. Provider thường làm luồng lấy dependency rõ hơn. [Nguồn: Lookup API](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/beans/factory/annotation/Lookup.html).

### 14.4. Vòng đời bean

Có thể nhớ theo các giai đoạn chính:

1. Spring đọc bean definition từ component scan, `@Bean` và các nguồn cấu hình.
2. Tạo instance; constructor injection diễn ra khi dựng instance, field/setter injection được điền ở giai đoạn populate.
3. Gọi các callback `Aware` thích hợp và xử lý trước initialization bằng `BeanPostProcessor`.
4. Initialization: thường gặp `@PostConstruct`, `InitializingBean.afterPropertiesSet()` và custom init method.
5. Xử lý sau initialization; nhiều cơ chế AOP trả về proxy để bean khác sử dụng.
6. Bean phục vụ ứng dụng.
7. Khi context đóng bình thường, thực thi callback hủy phù hợp như `@PreDestroy`, `DisposableBean.destroy()` và custom destroy method cho các bean được quản lý hủy.

Đây là sơ đồ nhớ mức khái niệm bằng các bước, không phải cam kết mọi extension point của Spring chạy theo một danh sách giản lược duy nhất. Ví dụ `@PostConstruct` cũng được hỗ trợ qua post-processor.

Không dựa vào `@PreDestroy` để bảo đảm nghiệp vụ luôn chạy khi máy bị tắt đột ngột. Không gọi method cần transaction qua `this` trong `@PostConstruct` và mong AOP tự áp dụng: bean đang khởi tạo, chưa phải lời gọi từ bên ngoài qua proxy hoàn chỉnh. [Nguồn: Spring bean lifecycle](https://docs.spring.io/spring-framework/reference/core/beans/factory-nature.html).

### Nói khi phỏng vấn

> “Singleton là một instance trên bean definition trong container; nó không tự thread-safe. Prototype tạo mới mỗi lần được container yêu cầu. Inject trực tiếp prototype vào singleton chỉ inject một lần nên muốn mỗi job một instance em dùng ObjectProvider hoặc Lookup. Vòng đời chính là tạo, inject, initialize, sử dụng và destroy.”

## 15. AOP, proxy và self-invocation

### 15.1. AOP giải quyết điều gì?

AOP tách các mối quan tâm dùng ở nhiều nơi như transaction, cache, metrics khỏi nghiệp vụ chính. **Aspect/advice** chứa hành vi bổ sung; **pointcut** chọn điểm áp dụng; **join point** trong Spring AOP chủ yếu là thực thi method.

Spring AOP thường dùng proxy: object bên ngoài nhận một object đại diện; proxy có thể mở transaction hoặc kiểm tra cache trước khi gọi target thật, rồi xử lý kết quả/lỗi sau đó.

Với transaction DB đồng bộ, proxy mở hoặc tham gia transaction trước khi gọi target. Khi target trả về bình thường, interceptor tiến hành commit; nếu exception phù hợp rollback rule đi ra, interceptor rollback. Vì commit nằm bên ngoài phần thân target method, return trong method chưa có nghĩa commit đã hoàn tất.

### 15.2. JDK dynamic proxy và class-based proxy

**JDK dynamic proxy:** proxy qua interface; lời gọi đi qua các method interface được proxy hỗ trợ.

**Class-based proxy:** Spring dùng cơ chế subclass, thường nhắc đến CGLIB; class `final` không thể subclass, method `final`/`private` không thể được override để advise theo cách này. Không nên khẳng định mọi Spring proxy đều có cùng giới hạn như nhau; cần xét loại proxy và visibility.

Spring Framework và Spring Boot có cấu hình mặc định khác nhau tùy lớp cấu hình/version. Khi debug, kiểm tra proxy thật; không dựa riêng vào việc class có interface để khẳng định runtime chắc chắn dùng JDK proxy. [Nguồn: Spring proxying](https://docs.spring.io/spring-framework/reference/core/aop/proxying.html).

### 15.3. Bẫy self-invocation

**[Minh họa]**

```java
@Service
class CheckoutService {
    public void checkout() {
        saveOrder();
    }

    @Transactional
    public void saveOrder() {
        // Ghi DB.
    }
}
```

Khi bên ngoài gọi `checkout()` qua proxy, thân method target gọi trực tiếp `this.saveOrder()`. Lời gọi nội bộ không đi vòng lại proxy, nên transaction advice trên `saveOrder()` không chạy theo cơ chế proxy mặc định.

Nói chính xác: nếu `checkout()` không có transaction sẵn thì `saveOrder()` không được mở transaction mới bởi annotation của chính nó. Tuy nhiên repository được gọi bên trong vẫn có thể tự mở transaction cho từng method. Nếu `checkout()` đã chạy trong transaction, `saveOrder()` vẫn thực thi trong transaction đang tồn tại, nhưng cấu hình riêng như `REQUIRES_NEW` của nó không được áp dụng qua lời gọi nội bộ.

### 15.4. Cách xử lý và dẫn chứng thực tế

Ưu tiên tách phần có ranh giới transaction thành bean khác và gọi qua dependency được inject. Có thể dùng `TransactionTemplate` nếu cần kiểm soát phạm vi bằng code. Self-injection hoặc `AopContext.currentProxy()` là phương án có thêm ràng buộc/coupling, không nên là lựa chọn mặc định để chữa mọi thiết kế.

**[Code hiện có]** Shop có cấu trúc phù hợp:

```java
// Trong OrderServiceImpl, một bean:
public OrderResponse createOrder(OrderCreateRequest request, String username) {
    return orderTransactionService.createOrderTransaction(request, username);
}

// Trong OrderTransactionService, bean khác:
@Transactional
public OrderResponse createOrderTransaction(OrderCreateRequest request, String username) {
    // Lưu đơn, trừ kho, cập nhật coupon...
}
```

Đây là ví dụ tốt để giải thích tránh self-invocation. Không cần viện dẫn một bug giả định mà project chưa gặp.

### 15.5. Cache và Async có bẫy tương tự

Trong chế độ proxy, gọi nội bộ method có `@Cacheable` hoặc `@Async` cũng có thể bỏ qua advice tương ứng. Annotation còn cần infrastructure hỗ trợ được bật và cấu hình phù hợp; gắn annotation không phải lúc nào cũng đủ.

`@Async` chuyển công việc sang executor; transaction theo thread của caller không tự chuyển sang thread mới. Method bất đồng bộ muốn transaction riêng phải được cấu hình/gọi qua cơ chế phù hợp.

**[Phạm vi project]** Chưa thấy `@Cacheable`, `@Async` hoặc custom `@Aspect` trong source được kiểm tra. Có Redis/Redisson dependency và có dùng Redisson lock không có nghĩa đã dùng Spring Cache.

### Nói khi phỏng vấn

> “Transactional hoạt động qua proxy nên lời gọi phải đi qua proxy thì advice mới chạy. A gọi B trong cùng object không áp dụng annotation riêng của B; nếu A đã có transaction thì B vẫn dùng ngữ cảnh đó. Shop của em tách OrderTransactionService để lời gọi từ OrderServiceImpl đi qua bean khác, làm ranh giới transaction rõ ràng hơn.”

## 16. Transactional: propagation, rollback và ranh giới

### 16.1. Một annotation, nhiều quyết định

`@Transactional` cung cấp metadata để infrastructure transaction áp dụng các chính sách như propagation, isolation, timeout, read-only và rollback rule. Nó không tự biến HTTP, Kafka và nhiều database thành một transaction phân tán.

Các method đang dùng annotation mặc định trong project thông thường có propagation `REQUIRED`, isolation `DEFAULT` và rollback rule mặc định nếu cấu hình ứng dụng không override. [Nguồn: Spring Transactional](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html).

### 16.2. Đủ bảy propagation

Trong các tình huống dưới đây, giả sử A gọi sang B **qua proxy**, dùng transaction manager phù hợp. Nếu gọi nội bộ bỏ qua proxy thì policy của B không được áp dụng như mô tả.

- **REQUIRED:** có transaction thì tham gia, chưa có thì tạo. Đây là mặc định cho nghiệp vụ nhiều bước cần commit cùng nhau.
- **REQUIRES_NEW:** tạm ngưng transaction đang có, mở transaction mới; commit/rollback độc lập với transaction ngoài.
- **SUPPORTS:** có thì tham gia, không có thì chạy không có transaction do method này yêu cầu.
- **NOT_SUPPORTED:** tạm ngưng transaction đang có và chạy ngoài transaction.
- **MANDATORY:** yêu cầu đã có transaction; thiếu thì báo lỗi.
- **NEVER:** yêu cầu không có transaction; có thì báo lỗi.
- **NESTED:** nếu có transaction, chạy phần lồng bằng savepoint khi được hỗ trợ; không có thì tạo transaction như REQUIRED.

Tên gọi và ý nghĩa được định nghĩa tại [Spring Propagation API](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/transaction/annotation/Propagation.html).

### 16.3. REQUIRED, REQUIRES_NEW và NESTED dễ nhầm thế nào?

**REQUIRED:** A lưu đơn, B lưu dòng hàng trong cùng physical transaction. Nếu B đánh dấu rollback-only thì A không thể cứu bằng việc chỉ catch exception rồi tiếp tục commit. Caller có thể nhận `UnexpectedRollbackException` khi transaction ngoài kết thúc.

**REQUIRES_NEW:** B có transaction riêng. B commit rồi A rollback thì thay đổi đã commit của B vẫn còn. Hợp một số log/audit độc lập, nhưng B không nên giả định nhìn được dòng A vừa insert mà chưa commit. Transaction ngoài có thể đang giữ connection/lock trong lúc transaction mới cần tài nguyên khác; phải tính pool capacity và nguy cơ chờ.

**NESTED:** B có thể rollback về savepoint, A tiếp tục nếu lỗi được xử lý phù hợp; nếu A rollback toàn bộ thì B vẫn bị hủy. Không phải commit độc lập. Hỗ trợ savepoint tùy transaction manager/driver; với JPA không nên mặc định nested rollback phục hồi đúng mọi trạng thái entity trong persistence context. [Nguồn: Spring propagation semantics](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/tx-propagation.html).

### 16.4. Isolation annotation được áp dụng lúc nào?

Isolation tùy chỉnh chủ yếu có tác dụng khi một transaction mới thực sự được tạo. Method B dùng REQUIRED tham gia transaction A thường dùng isolation của transaction A, không tự nâng mức chỉ vì B ghi `isolation = SERIALIZABLE`. Có cấu hình validation để từ chối một số trường hợp không tương thích.

```java
// Minh họa: không phải cấu hình hiện có của project.
@Transactional(
    propagation = Propagation.REQUIRED,
    isolation = Isolation.READ_COMMITTED,
    rollbackFor = IOException.class,
    timeout = 5
)
public void importOrders() throws IOException {
    // Nghiệp vụ DB.
}
```

Transaction timeout không phải cơ chế universal ngắt mọi HTTP call hay `Thread.sleep()` sau đúng số giây; cần cấu hình timeout riêng cho các client/network và hiểu driver hỗ trợ thế nào.

### 16.5. Rollback mặc định và rollbackFor

Với cấu hình mặc định thông thường, Spring rollback cho `RuntimeException` và `Error`; checked exception không mặc nhiên gây rollback. Có thể thay bằng `rollbackFor`, `noRollbackFor` hoặc cấu hình default toàn cục, nên khi debug phải đọc cấu hình thật. [Nguồn: Spring rollback rules](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/rolling-back.html).

```java
// Minh họa: rollback cả khi lỗi đọc file là checked exception.
@Transactional(rollbackFor = IOException.class)
public void importData() throws IOException {
    // Ghi DB rồi đọc dữ liệu từ file...
}
```

Nếu catch exception trong method rồi trả về bình thường, interceptor không còn nhìn thấy exception đó để áp rollback rule. Tuy nhiên DB/provider hoặc transaction con có thể đã đánh dấu rollback-only; catch không tự xóa cờ này. Nên có chính sách rõ: ném lại exception, chuyển thành exception phù hợp hoặc chủ động đánh dấu rollback khi thật sự cần.

`rollbackFor = Exception.class` không phải “thuốc chữa tất cả”: nó không chữa self-invocation, không hoàn tác email đã gửi và không biến message Kafka thành bản ghi DB.

### 16.6. readOnly có nghĩa gì?

`@Transactional(readOnly = true)` thể hiện ý định đọc và có thể giúp framework/provider/driver tối ưu. Không dùng nó thay cho quyền DB chỉ đọc; không mặc định mọi thao tác ghi đều bị từ chối. Hành vi thực tế phụ thuộc transaction manager và database.

**[Code hiện có]** `ProductServiceImpl.getAllProducts()` và `getProductById()` dùng readOnly; các method tạo/sửa/xóa dùng transaction ghi.

### 16.7. Đặt transaction ở Service hay Controller?

Thông thường đặt ở **service/application use case**, nơi biết các bước nào cần thành công cùng nhau. Controller nhận/validate request và chuyển đổi response; repository xử lý truy cập dữ liệu. Transaction chỉ ở từng repository có thể làm các bước nghiệp vụ commit rời rạc.

“Không bao giờ đặt ở Controller” là lời khuyên kiến trúc quá tuyệt đối. Về kỹ thuật Spring có thể áp dụng ở bean phù hợp, nhưng đặt ở service giúp ranh giới nhất quán, tái sử dụng khi gọi từ REST, Kafka hoặc scheduler và tránh transaction ôm quá nhiều việc của web layer.

### 16.8. Tránh chờ network trong transaction đang giữ tài nguyên DB

**[Code hiện có]** Microservice `OrderServiceImpl.createOrder()` có `@Transactional` rồi gọi `ProductClient.getProductsByIds()`; implementation dùng WebClient `.block()`. Việc đọc sản phẩm nằm trong phạm vi method transaction. Thời điểm thực sự lấy physical connection còn tùy cấu hình/JPA, nên không khẳng định DB connection luôn đã bị giữ từ dòng đầu method. Sau khi đã truy cập DB, chờ network trong cùng transaction có thể giữ connection và lock lâu hơn cần thiết.

**[Đề xuất]** Dùng orchestration bên ngoài để gọi dữ liệu cần thiết, rồi gọi bean transaction ngắn cho phần ghi DB. Vì sản phẩm/stock ở service khác có thể đổi sau lúc đọc, phải dùng cơ chế reserve/deduct nguyên tử tại service sở hữu stock, trạng thái đơn và bù trừ/retry phù hợp. Đưa HTTP ra ngoài transaction đơn thuần chưa giải quyết race condition phân tán.

**[Code hiện có]** Shop `NotificationScheduler.sendPendingNotifications()` có `@Transactional`, query DB rồi gửi email trong vòng lặp. Ở đây network call xảy ra sau khi query DB và nằm trong transaction có thể kéo dài. Email đã gửi cũng không rollback được nếu commit DB thất bại, nên có khả năng gửi trùng ở lần chạy tiếp theo.

**[Đề xuất]** Claim một batch notification bằng transaction ngắn, gửi ngoài transaction, rồi cập nhật kết quả bằng transaction ngắn khác; có retry/backoff và chính sách chống trùng. Nhiều worker cần cơ chế claim nguyên tử để tránh cùng gửi một notification.

### 16.9. Transaction không tự truyền qua thread hay service

Với transaction imperative thông thường, context gắn với thread. Gọi sang `@Async`/thread mới hoặc gửi HTTP/Kafka không tự mang cùng DB transaction sang bên kia. Consumer `OrderCreatedConsumer` gọi `ProductService.updateStock()` qua bean sẽ có transaction cục bộ tại product-service, khác transaction đã tạo đơn ở order-service.

### Nói khi phỏng vấn

> “Em đặt transaction ở service theo ranh giới use case. REQUIRED tham gia transaction hiện có, REQUIRES_NEW độc lập, còn NESTED dùng savepoint nếu được hỗ trợ. Em chú ý rollback rule, self-invocation và giữ transaction ngắn; network call không được DB rollback cùng, nên luồng microservice cần thêm trạng thái, idempotency và outbox thay vì chỉ thêm annotation.”

## 17. Request lifecycle, Filter và Interceptor

### 17.1. Luồng request Spring MVC

Với một request REST đồng bộ thông thường, có thể diễn đạt tuần tự như sau:

1. Servlet container nhận HTTP request và chạy filter chain; Spring Security có thể xử lý/xử lý từ chối ở đây.
2. `DispatcherServlet` nhận request và dùng `HandlerMapping` tìm handler phù hợp cùng các interceptor.
3. Interceptor chạy `preHandle()` trước handler. `HandlerAdapter` điều phối việc gọi controller, resolve tham số, bind dữ liệu và validation.
4. Controller gọi service; service gọi repository/DB hoặc các dependency khác.
5. Với `@ResponseBody`/`@RestController`, return value được xử lý và `HttpMessageConverter` ghi response, ví dụ JSON.
6. Các callback sau xử lý và completion chạy theo lifecycle phù hợp; filter có thể thực thi phần sau `chain.doFilter()` khi stack lời gọi quay ra.

Request lỗi có thể được `HandlerExceptionResolver`/`@ControllerAdvice` xử lý, nhưng lỗi xảy ra trước DispatcherServlet ở filter không tự đi vào mọi handler của MVC. Async request có lifecycle khác nên sơ đồ nhớ trên là trường hợp đồng bộ thông thường. [Nguồn: Spring DispatcherServlet](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-servlet.html).

### 17.2. Filter

Filter thuộc Servlet API và bao quanh việc gọi servlet. Phù hợp việc tác động rộng lên request/response: correlation ID, logging thời gian, CORS, authentication qua security filter chain, giới hạn request theo chính sách.

Filter không trực tiếp có `HandlerMethod` theo cách interceptor có sau khi mapping. Nhưng nói “Filter ở ngoài Spring Context nên không inject Spring bean được” là sai: filter có thể được Spring quản lý hoặc ủy quyền cho Spring bean qua `DelegatingFilterProxy`. [Nguồn: Spring Security servlet architecture](https://docs.spring.io/spring-security/reference/servlet/architecture.html).

Nếu nói về XSS, cần hiểu filter “lọc chuỗi đầu vào” không phải giải pháp đầy đủ. Encode đầu ra đúng ngữ cảnh và xử lý HTML có chủ đích mới là những phần quan trọng; không quảng bá một filter thay thế toàn bộ thiết kế bảo vệ web.

### 17.3. Interceptor

Interceptor thuộc Spring MVC, biết handler đã được chọn. Hợp logic phụ thuộc controller như đọc annotation nghiệp vụ, kiểm tra một điều kiện bổ sung, hoặc thêm thông tin model cho MVC view.

- `preHandle()`: chạy trước handler; trả false có thể dừng chuỗi xử lý tiếp.
- `postHandle()`: sau handler thành công và trước render view trong luồng MVC truyền thống.
- `afterCompletion()`: xử lý sau khi request hoàn tất trong lifecycle tương ứng, thường dùng cleanup/logging.

Với REST, response body có thể đã được ghi trong quá trình handler adapter xử lý return value trước `postHandle()`, nên không mặc định callback này là chỗ chỉnh JSON response. Có thể cần `ResponseBodyAdvice` cho use case đó.

**Authorization:** không mặc định nên tự viết toàn bộ phân quyền trong Interceptor. Spring khuyến nghị áp dụng security sớm qua Spring Security/filter chain, kết hợp method security khi phù hợp; interceptor có thể không bao phủ mọi đường truy cập theo cách bạn nghĩ. [Nguồn: Spring MVC interception](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-servlet/handlermapping-interceptor.html).

### 17.4. Dẫn chứng trong hai project

**[Code hiện có]** Shop có [WebSecurityConfig.java](../ecomerc%20shop/ecommerc_shop_backend/src/main/java/org/example/ecommerc_shop/config/WebSecurityConfig.java) khai báo `SecurityFilterChain`, phân quyền `/api/v1/admin/**` cho ADMIN, `/api/v1/shipper/**` cho SHIPPER và dùng HTTP Basic. Đây là ví dụ authentication/authorization thực hiện qua Spring Security ở servlet stack.

**[Code hiện có]** Microservice gateway có [SecurityConfig.java](api-gateway/src/main/java/org/example/apigateway/config/SecurityConfig.java) dùng `ServerHttpSecurity`, `SecurityWebFilterChain`, JWT và reactive converter. Đây là reactive stack: nói về `WebFilter`/WebFlux, **không áp nguyên lifecycle DispatcherServlet của MVC vào gateway này**.

Các product/order controller dùng mô hình MVC. Cùng một hệ thống có thể có gateway reactive và các backend service MVC; không nên gọi toàn bộ hệ thống là reactive chỉ vì có WebClient.

### Nói khi phỏng vấn

> “Ở MVC, request qua filter chain rồi DispatcherServlet tìm handler và interceptor, sau đó gọi controller và service. Filter ở tầng Servlet còn interceptor biết handler MVC. Project shop dùng SecurityFilterChain cho HTTP Basic và phân quyền; gateway microservice lại dùng SecurityWebFilterChain với JWT nên em phân biệt servlet stack với reactive stack.”

## 18. Spring Boot và auto-configuration

### 18.1. Spring Boot bổ sung gì cho Spring?

Spring cung cấp container, DI, AOP, transaction và web framework. Spring Boot giúp khởi động/cấu hình ứng dụng thuận tiện hơn qua auto-configuration, starter, dependency management và cách chạy ứng dụng độc lập.

Starter gom các dependency phù hợp cho một nhóm tính năng. Auto-configuration là logic tạo cấu hình/bean khi điều kiện đáp ứng. Hai khái niệm liên quan nhưng không phải cùng một thứ.

### 18.2. SpringBootApplication gồm gì?

`@SpringBootApplication` kết hợp các vai trò:

- `@SpringBootConfiguration`: đánh dấu cấu hình của ứng dụng Boot; bản thân liên quan đến `@Configuration`.
- `@EnableAutoConfiguration`: bật cơ chế auto-configuration.
- `@ComponentScan`: tìm component trong package của class ứng dụng và các package con theo cấu hình mặc định.

Vì vậy cách nói “gộp Configuration, ComponentScan, EnableAutoConfiguration” là bản tóm tắt; tên annotation trực tiếp chính xác hơn là `@SpringBootConfiguration`. Đặt class main ở package gốc hợp lý để service/repository/entity liên quan được phát hiện theo các cơ chế scan của Boot. [Nguồn: SpringBootApplication](https://docs.spring.io/spring-boot/reference/using/using-the-springbootapplication-annotation.html).

**[Code hiện có]** `MicroApplication` ở `org.example.micro` có `@SpringBootApplication`; controller, service, repository nằm trong các package con. Shop có `EcommercShopApplication` dưới `org.example.ecommerc_shop`, đồng thời bật scheduling và retry.

### 18.3. Auto-configuration hoạt động ra sao?

Boot nạp các auto-configuration candidate được đăng ký bởi thư viện, ở các phiên bản hiện đại thường qua `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`. Sau đó các điều kiện quyết định cấu hình nào thực sự có hiệu lực.

- `@ConditionalOnClass`: class cần thiết có trên classpath.
- `@ConditionalOnMissingBean`: chưa có bean phù hợp do ứng dụng hay cấu hình khác cung cấp.
- `@ConditionalOnProperty`: property đáp ứng điều kiện.
- Ngoài ra có điều kiện về loại ứng dụng web và nhiều yếu tố khác.

“Có JPA starter là tự cấu hình mọi thứ thành công” còn thiếu: phải có driver, cấu hình kết nối hoặc embedded DB phù hợp, và các điều kiện khác. Nếu điều kiện không đúng, có thể không tạo bean hoặc startup thất bại vì thiếu cấu hình bắt buộc. [Nguồn: Boot auto-configuration](https://docs.spring.io/spring-boot/reference/features/developing-auto-configuration.html).

### 18.4. Dẫn chứng và cách debug

**[Code hiện có]** Product/order/shop có JPA starter và MySQL driver trong `pom.xml`. Đây là cơ sở để Boot cấu hình hạ tầng JPA/DataSource khi đủ điều kiện; không chứng minh DB đang chạy hoặc kết nối thành công.

`AuditingConfig.auditorProvider()` là ví dụ **bean do ứng dụng tự khai báo bằng `@Bean`**, không phải mọi bean trong context đều do auto-configuration tạo. Tương tự shop tự cung cấp `PasswordEncoder` và `SecurityFilterChain`.

**Khi debug:** xem condition evaluation report qua chế độ debug; kiểm tra dependency, property, bean hiện có và package scan. Nếu dùng Actuator conditions endpoint thì cần quản lý quyền truy cập phù hợp. Không xóa cấu hình ngẫu nhiên chỉ vì Boot “tự động”.

### Nói khi phỏng vấn

> “Boot tự cấu hình có điều kiện dựa trên classpath, property và bean đã có. SpringBootApplication bật component scan, cấu hình ứng dụng và auto-configuration. Trong project, starter JPA cùng MySQL driver cung cấp đầu vào để Boot dựng hạ tầng persistence; khi lỗi em xem condition report và cấu hình thực tế thay vì nghĩ starter tự làm mọi thứ.”

## 19. JPA: lifecycle, dirty checking, save và flush

### 19.1. Phân biệt JPA, Hibernate và Spring Data JPA

**JPA/Jakarta Persistence:** đặc tả và API cho persistence/ORM. **Hibernate:** một implementation phổ biến của đặc tả cùng các tính năng riêng. **Spring Data JPA:** lớp abstraction giúp dùng repository, derived query, pagination và tích hợp JPA thuận tiện hơn.

`JpaRepository` không phải một database và `save()` không phải lúc nào cũng tương đương một câu `INSERT` chạy ngay.

### 19.2. Bốn trạng thái entity

**Transient/New:** vừa `new Product()`, chưa được persistence context quản lý, chưa được lên lịch persist trong context đó.

**Managed:** đang gắn với persistence context. Thay đổi thuộc tính có thể được ORM phát hiện và đồng bộ khi flush; không cần gọi `save()` mỗi lần sửa một managed entity.

**Detached:** từng là persistent nhưng hiện không còn được context quản lý, ví dụ sau `detach()`, `clear()` hoặc context đóng. Sửa object detached không tự làm DB update. Có ID không có nghĩa object đang managed.

**Removed:** entity được đánh dấu xóa trong persistence context; thao tác DELETE được đồng bộ theo flush/transaction. Đây là khái niệm JPA, khác soft delete.

`merge(detached)` sao chép state vào một managed instance và trả instance managed đó; object detached ban đầu không tự biến thành managed. Khi cần làm việc tiếp, dùng object được trả về. [Nguồn: Jakarta EntityManager](https://jakarta.ee/specifications/persistence/3.2/apidocs/jakarta.persistence/jakarta/persistence/EntityManager.html).

### 19.3. Dirty checking và first-level cache

Persistence context giữ danh tính entity: cùng một identity trong context thường được đại diện bởi cùng managed instance. ORM theo dõi thay đổi và chuyển thành SQL lúc flush. Đây là nền tảng first-level cache và dirty checking; không phải cache dùng chung toàn ứng dụng và không có nghĩa lần nào gọi repository cũng không chạy SQL.

**[Code hiện có]** Microservice `OrderServiceImpl.updateStatus()`:

```java
@Transactional
public void updateStatus(String orderId) {
    Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
    order.setStatus("PREPARED");
}
```

Không có `orderRepository.save(order)` lần nữa. Entity vừa được load trong transaction đang managed; khi flush, thay đổi status được ghi xuống DB nếu transaction hoàn tất đúng.

`ProductServiceImpl.updateProduct()` và `deleteProduct()` cũng dựa trên managed entity. Tuy nhiên `deleteProduct()` chỉ `setIsDeleted(true)`: đó là **UPDATE soft delete**, entity không vì thế đi vào trạng thái JPA Removed. Những query cần ẩn dữ liệu đã xóa phải áp dụng filter tương ứng.

### 19.4. save()

Spring Data JPA quyết định entity mới hay đã tồn tại theo chiến lược phát hiện, thường xét version/ID và có thể dùng `Persistable`. Entity mới dùng `EntityManager.persist()`, còn trường hợp đã có dùng `merge()`. Không nên đơn giản hóa thành “ID khác null thì lúc nào cũng UPDATE”. [Nguồn: Spring Data persisting entities](https://docs.spring.io/spring-data/jpa/reference/jpa/entity-persistence.html).

`save()` **không cam kết SQL luôn chạy ngay**, cũng không cam kết “chỉ chạy SQL đúng cuối transaction”. ORM có thể flush trước một số query, khi flush được yêu cầu, khi commit, hoặc cần INSERT sớm cho chiến lược ID như IDENTITY.

Nếu không có transaction service bao ngoài, method repository `save()` có thể tự mở và kết thúc transaction của nó, nên SQL/commit có thể đã diễn ra trước khi caller nhận kết quả. Luôn xét transaction thực tế thay vì chỉ nhìn tên method.

### 19.5. saveAndFlush()

`saveAndFlush(entity)` lưu entity theo cơ chế save rồi flush persistence context. Các thay đổi đang chờ của những managed entity khác cũng có thể được flush; không phải chỉ duy nhất entity truyền vào. Nó **không tự kết thúc transaction ngoài**. [Nguồn: JpaRepository API](https://docs.spring.io/spring-data/jpa/reference/api/java/org/springframework/data/jpa/repository/JpaRepository.html).

```java
// Minh họa, giả sử method được gọi qua transaction proxy.
@Transactional
public void demoFlush() {
    Order order = new Order();
    // Thiết lập đủ dữ liệu bắt buộc...
    orderRepository.saveAndFlush(order);
    throw new IllegalStateException("rollback demo");
}
```

SQL đã được gửi xuống DB ở flush nhưng exception vẫn làm transaction rollback theo rule mặc định. Các transaction khác nhìn thấy gì còn tùy isolation; **flush không bằng commit**.

Use case có lý do: cần phát hiện sớm một số lỗi constraint, hoặc cần DB nhận thay đổi trước bước native query/procedure tiếp theo. Vẫn phải xét constraint có thể deferred và ngữ cảnh cụ thể. Không dùng `saveAndFlush()` mọi nơi vì có thể làm giảm cơ hội gom/batch SQL.

### 19.6. Có cần saveAndFlush để lấy ID?

Không mặc định. Project dùng `GenerationType.UUID`, provider thường có thể cấp ID khi persist/save mà không cần đợi DB insert như IDENTITY. Với IDENTITY, ORM có thể phải insert sớm để nhận ID; vì vậy phát biểu “cần ID thì luôn saveAndFlush” cũng sai.

Write-behind là trì hoãn đồng bộ thay đổi đến flush; **JDBC batching là tối ưu riêng**, cần cấu hình và điều kiện thích hợp. Nhiều lần `save()` không bảo đảm tự được gộp thành một SQL duy nhất hoặc một batch. [Nguồn: Hibernate flushing và batching](https://docs.hibernate.org/orm/7.1/userguide/html_single/#flushing).

### Nói khi phỏng vấn

> “Entity có các trạng thái new, managed, detached và removed. Managed entity được dirty checking, nên updateStatus trong project chỉ cần load rồi đổi trạng thái trong transaction. Save dùng persist hoặc merge tùy entity; saveAndFlush thêm bước đồng bộ xuống DB, nhưng flush chưa commit và vẫn rollback được. Lấy ID có cần insert sớm hay không còn phụ thuộc chiến lược sinh ID.”

## 20. SOLID, đặc biệt Dependency Inversion

SOLID là nhóm nguyên tắc giúp code dễ thay đổi và kiểm thử. Không phải luật bắt mọi class phải có interface hay mọi method chỉ được vài dòng.

### 20.1. S — Single Responsibility Principle

Một module nên có một nhóm trách nhiệm nhất quán, tức một lý do chính để thay đổi từ góc nhìn nghiệp vụ. Không có nghĩa một class chỉ được có một method.

**[Code hiện có]** Controller nhận HTTP request, service xử lý use case, repository truy cập DB, mapper chuyển entity/DTO. Sự phân chia này hỗ trợ SRP.

**[Đề xuất]** Microservice `ProductServiceImpl.updateStock()` đang chứa điều phối Redis lock, cập nhật tồn kho và Kafka publish; có thể tách policy tồn kho và event publication/outbox để thay đổi cơ chế hạ tầng không cuốn toàn bộ nghiệp vụ vào. Việc tách cần phục vụ thay đổi thật, không chỉ tăng số class.

### 20.2. O — Open/Closed Principle

Module nên cho phép mở rộng hành vi mà hạn chế sửa phần logic đã ổn định. Ví dụ thêm cách tính giảm giá mới bằng strategy thay vì rải thêm if/else ở nhiều service.

**[Code hiện có]** Shop `OrderTransactionService` đang phân nhánh `PERCENTAGE` và `FIXED_AMOUNT`. **[Đề xuất]** Nếu số loại khuyến mãi tăng, có thể có `DiscountPolicy` và các implementation. Khi chỉ có hai nhánh đơn giản và ít thay đổi, chưa chắc tách strategy đã mang lại lợi ích.

### 20.3. L — Liskov Substitution Principle

Implementation thay thế abstraction phải giữ các cam kết mà caller dựa vào: điều kiện đầu vào, ý nghĩa đầu ra, side effect và lỗi được công bố. “Cùng implement một interface” mới là khớp kiểu, chưa chứng minh đúng hành vi.

**[Minh họa theo project]** Một fake `ProductClient` dùng trong test nên giữ ý nghĩa ID, giá, stock và quy ước sản phẩm không tồn tại tương thích với contract. Nếu implementation mới âm thầm bỏ lỗi mà caller cần xử lý, thay thế có thể phá nghiệp vụ dù code vẫn compile.

### 20.4. I — Interface Segregation Principle

Caller không nên phải phụ thuộc những method không cần. Thay vì interface quá lớn cho mọi thao tác user, shipping, product và payment, chia theo trách nhiệm và nhóm người dùng interface.

**[Code hiện có]** Có các interface `ProductService`, `CategoryService`, `OrderService` riêng thay vì một interface tổng hợp toàn hệ thống. **[Đề xuất]** Nếu nhu cầu đọc catalog và trừ kho phát triển độc lập, cân nhắc tách contract client theo use case; không bắt buộc tách chỉ vì hiện tại có hai method.

### 20.5. D — Dependency Inversion Principle

Có hai ý chính:

1. Module nghiệp vụ cấp cao và module chi tiết cấp thấp nên phụ thuộc vào abstraction.
2. Abstraction nên thể hiện điều nghiệp vụ cần, không bị buộc bởi implementation cụ thể; các chi tiết hạ tầng thích nghi theo abstraction đó.

**[Code hiện có]** Microservice `OrderServiceImpl` phụ thuộc `ProductClient` interface. [ProductClientImpl.java](order-service/src/main/java/org/example/orderservice/clients/impl/ProductClientImpl.java) là implementation dùng WebClient. Nghiệp vụ tạo đơn gọi contract như `getProductsByIds()` thay vì tự dựng request HTTP ngay trong vòng lặp nghiệp vụ.

Khi thay WebClient bằng implementation khác hoặc fake trong unit test, caller có thể giữ nguyên nếu contract vẫn đúng. Tuy nhiên `OrderServiceImpl` vẫn inject `KafkaTemplate` trực tiếp; đây là ví dụ code **chưa tách hoàn toàn** nghiệp vụ khỏi chi tiết publish. Không nên nói project đã đạt tuyệt đối DIP chỉ vì có vài interface.

### 20.6. DIP, DI và IoC có phải một không?

**DIP** là nguyên tắc định hướng quan hệ phụ thuộc trong thiết kế. **DI** là kỹ thuật truyền dependency từ ngoài vào. **IoC** là khái niệm rộng hơn về chuyển quyền điều khiển cho framework/container.

Bạn có thể dùng constructor DI nhưng vẫn inject thẳng `ConcreteHttpClient`, lúc đó chưa tận dụng đầy đủ DIP. Ngược lại, code dùng abstraction đúng DIP vẫn có thể tự nối các object ở composition root mà không cần Spring.

### Nói khi phỏng vấn

> “SOLID giúp giới hạn ảnh hưởng khi thay đổi. Chữ D là nghiệp vụ và hạ tầng phụ thuộc contract thay vì nghiệp vụ gắn chặt implementation. Trong project, OrderServiceImpl dùng ProductClient interface còn HTTP nằm trong ProductClientImpl. DI là cách Spring truyền implementation vào, còn IoC là container quản lý quá trình tạo và nối object; ba khái niệm liên quan nhưng không đồng nghĩa.”

## 21. Kể về project khi phỏng vấn

### 21.1. Cách trả lời một chủ đề trong 30–60 giây

Giữ bốn ý: **đó là gì — giải quyết vấn đề gì — ví dụ mình dùng — đánh đổi/giới hạn**. Nếu người phỏng vấn hỏi sâu mới mở rộng cơ chế, SQL hoặc cách đo.

Ví dụ với optimistic lock: “Đây là cách phát hiện cập nhật đồng thời bằng version. Trong shop, ProductVariant có Version để tránh ghi đè tồn kho. Nếu hai transaction cùng đọc một version thì chỉ một update thắng; bên còn lại rollback và có chính sách retry. Cách này hợp khi ít conflict, còn tranh chấp cao có thể phải chọn cách khác.”

Không bắt đầu bằng danh sách annotation dài mà chưa nói vấn đề. Không biến phương án đề xuất trong tài liệu thành kinh nghiệm đã triển khai hoặc kết quả đã đo.

### 21.2. Câu chuyện 1: thiết kế đặt hàng trong shop

> “Luồng tạo đơn của em đi từ OrderServiceImpl sang OrderTransactionService. Trong một transaction, em lấy cart item cùng variant và product bằng fetch join, kiểm tra kho, tính tiền, lưu đơn và dòng hàng, trừ kho và cập nhật coupon. ProductVariant dùng optimistic version; coupon dùng pessimistic lock vì có giới hạn lượt. Em giữ giá đơn vị trên dòng hàng để bảo toàn giá lúc mua. Việc gọi sang bean transaction riêng cũng giúp đi qua proxy của Spring.”

Nếu hỏi tiếp về retry:

> “Code có Retryable cho optimistic locking exception với số lần thử giới hạn. Em cần kiểm chứng thứ tự advice và thực tế mỗi lần retry có transaction mới. Retry phải tải lại dữ liệu rồi kiểm tra lại điều kiện; không chỉ gửi lại cùng một state cũ.”

Đây là mô tả dựa trên source. Chưa có số liệu thử đồng thời hoặc throughput nên không nói “đã chống oversell tuyệt đối trong mọi luồng”. Những luồng ghi khác, bulk update và cấu hình runtime vẫn cần được kiểm tra.

### 21.3. Câu chuyện 2: phát hiện và xử lý N+1

> “Ở API danh sách sản phẩm của microservice, entity category là LAZY nhưng mapper cần categoryName. Vì thế có nguy cơ ORM tải thêm từng category khi map response. Cách em kiểm chứng là bật query statistics trong test và đếm SQL với nhiều sản phẩm thuộc các category khác nhau. Hướng xử lý là entity graph cho category hoặc DTO projection. Trong shop đã có fetch join cart item và entity graph user trên danh sách đơn để tải đúng dữ liệu cần dùng.”

Khi chưa chạy đo, dùng từ **“nguy cơ”, “cách kiểm chứng”, “hướng xử lý”**. Không tự đưa con số “giảm 100 query còn 1” vào CV/phỏng vấn.

### 21.4. Câu chuyện 3: cải thiện tính nhất quán ở microservice

> “Order-service lưu đơn và phát sự kiện để product-service xử lý tồn kho. Em nhận ra transaction DB không bảo đảm nguyên tử với Kafka. Hướng cải thiện là outbox cho publish, consumer idempotent và trạng thái đơn để xử lý thành công/thất bại hoặc bù trừ. Ở tồn kho em cũng sẽ bảo vệ cập nhật ngay tại database, vì kiểm tra stock ở order-service trước đó không bảo đảm stock vẫn còn khi event được xử lý.”

Code hiện có đặt đơn ở trạng thái `NEW` rồi consumer cập nhật `PREPARED` sau sự kiện `lock_product`; các trạng thái bù trừ/thất bại và cơ chế outbox là **đề xuất mở rộng**, không phải những tính năng đã xác nhận trong source.

### 21.5. Thứ tự ôn tập có ích nhất

1. Nắm transaction, dirty checking, flush/commit, lock và ranh giới DB/Kafka qua luồng đặt hàng.
2. Nắm LAZY, N+1, fetch plan và pagination qua API sản phẩm/giỏ hàng.
3. Nắm index và EXPLAIN bằng cách tự giải thích một query có filter, sort và LIMIT.
4. Nắm IoC/DI, proxy, self-invocation, scope và auto-configuration qua các bean đang có.
5. Tập tradeoff của schema pattern, partition/shard và SOLID sau khi đã kể được luồng chính rõ ràng.

## 22. Các câu dễ trả lời sai

1. **“Prototype inject vào singleton sẽ thành singleton.”** Scope không đổi; singleton giữ instance đã được inject một lần. Dùng provider/lookup nếu cần lấy mới.
2. **“B có Transactional nhưng A gọi nội bộ nên B chắc chắn không có transaction.”** Annotation riêng của B bị bỏ qua theo proxy mode; B vẫn có thể chạy trong transaction sẵn của A hoặc gọi repository có transaction riêng.
3. **“Save chỉ đẩy SQL vào cuối transaction.”** Có thể flush sớm vì query, yêu cầu flush hoặc chiến lược ID; repository cũng có thể tự commit nếu không có transaction ngoài.
4. **“SaveAndFlush là commit.”** Flush đồng bộ SQL; transaction ngoài vẫn có thể rollback.
5. **“Cần ID thì phải SaveAndFlush.”** Phụ thuộc chiến lược sinh ID; UUID trong project thường không cần DB insert mới tạo được ID.
6. **“Có write-behind thì tự có JDBC batch.”** Là hai cơ chế khác nhau; batch cần điều kiện và cấu hình phù hợp.
7. **“EAGER hết N+1.”** Không bảo đảm một query; cần fetch plan theo use case và kiểm chứng SQL.
8. **“Dùng fetch join mọi nơi là tối ưu.”** Collection fetch có thể nhân dòng và xung đột pagination; chỉ tải dữ liệu cần.
9. **“Dùng index chắc chắn nhanh.”** Quét phần lớn index rồi tra bảng có thể đắt hơn table scan.
10. **“Index `(a,b)` giống `(b,a)`.”** Thứ tự khóa ảnh hưởng tìm kiếm, range và sort; thứ tự biểu thức trong WHERE lại không quyết định thứ tự index.
11. **“Covering là một index đặc biệt độc lập query.”** Coverage phụ thuộc tập cột query cần.
12. **“Hash hỗ trợ range vì tìm nhanh.”** Hash không giữ thứ tự khóa.
13. **“Full-text giúp mọi LIKE có wildcard.”** Full-text có semantics token và cú pháp riêng.
14. **“REPEATABLE READ là stock luôn đúng.”** Cần xét read-modify-write và cơ chế bảo vệ invariant, không chỉ tên isolation.
15. **“REQUIRES_NEW giống NESTED.”** Một bên là transaction độc lập; một bên thường là savepoint trong transaction ngoài khi được hỗ trợ.
16. **“Catch exception rồi là transaction commit được.”** Có thể đã rollback-only; còn phụ thuộc lỗi và đường đi qua interceptor.
17. **“ReadOnly cấm ghi tuyệt đối.”** Đây là metadata/hint theo infrastructure, không thay quyền truy cập DB.
18. **“Method có Transactional thì SQL, Kafka và HTTP rollback cùng nhau.”** Chỉ tài nguyên tham gia transaction tương ứng mới có bảo đảm đó.
19. **“Filter không inject Spring bean được.”** Có thể được Spring quản lý hoặc ủy quyền đến bean; tầng Servlet không đồng nghĩa nằm ngoài mọi integration của Spring.
20. **“Authorization luôn đặt ở interceptor.”** Spring Security/filter chain và method security thường là cơ chế phù hợp; interceptor không thay thế toàn bộ security layer.
21. **“PageRequest hoặc Slice là cursor pagination.”** PageRequest thường dùng offset; Slice chỉ thay yêu cầu metadata tổng, không quyết định chiến lược seek.
22. **“Cursor luôn O(1), không thể trùng hoặc sót.”** Cần index/sort/filter phù hợp; nó không tự giữ snapshot xuyên request.
23. **“Microservice có DB riêng là sharding.”** Đây là tách domain; sharding chia cùng tập dữ liệu theo shard key.
24. **“DI, IoC và DIP là một.”** Lần lượt là kỹ thuật inject, khái niệm chuyển quyền điều khiển và nguyên tắc thiết kế dependency.
25. **“Soft delete là trạng thái Removed của JPA.”** Đổi cờ xóa vẫn là update; trạng thái Removed liên quan thao tác xóa của persistence context.

## 23. Danh sách dẫn chứng trong source

### 23.1. MiCROSERVICE

- [Product.java](product-service/src/main/java/org/example/micro/entity/Product.java): UUID ID, giá, stock, `ManyToOne(LAZY)` đến category.
- [Category.java](product-service/src/main/java/org/example/micro/entity/Category.java): quan hệ cha `parent_id`, ví dụ Adjacency List.
- [BaseEntity.java](product-service/src/main/java/org/example/micro/entity/BaseEntity.java): các cột soft delete và `created_date` dùng trong ví dụ SQL.
- [ProductRepository.java](product-service/src/main/java/org/example/micro/repository/ProductRepository.java): truy vấn phân trang và khai báo `findByIdForUpdate()` với pessimistic lock.
- [ProductServiceImpl.java](product-service/src/main/java/org/example/micro/service/impl/ProductServiceImpl.java): constructor injection, transaction, dirty checking, phân trang, cập nhật stock dùng Redisson; lưu ý method cập nhật stock hiện gọi `findByIdIn()`.
- [ProductMapper.java](product-service/src/main/java/org/example/micro/mapper/ProductMapper.java): đọc `category.name` khi map DTO; dẫn chứng nguy cơ N+1.
- [ProductController.java](product-service/src/main/java/org/example/micro/controller/ProductController.java): API dùng `PageRequest`, chưa chỉ định sort trong method danh sách hiện tại.
- [Order.java](order-service/src/main/java/org/example/orderservice/entity/Order.java) và [OrderItem.java](order-service/src/main/java/org/example/orderservice/entity/OrderItem.java): tổng lưu sẵn, snapshot giá và association order.
- [OrderServiceImpl.java](order-service/src/main/java/org/example/orderservice/service/impl/OrderServiceImpl.java): tạo đơn có transaction, gọi ProductClient, lưu DB, phát Kafka; `updateStatus()` minh họa dirty checking.
- [ProductClient.java](order-service/src/main/java/org/example/orderservice/clients/ProductClient.java) và [ProductClientImpl.java](order-service/src/main/java/org/example/orderservice/clients/impl/ProductClientImpl.java): abstraction và implementation dùng WebClient.
- [OrderCreatedConsumer.java](product-service/src/main/java/org/example/micro/consumers/OrderCreatedConsumer.java): xử lý event và gọi service cập nhật kho.
- [ProductLockedConsumer.java](order-service/src/main/java/org/example/orderservice/consumers/ProductLockedConsumer.java): nhận event rồi gọi `orderService.updateStatus()`.
- [MicroApplication.java](product-service/src/main/java/org/example/micro/MicroApplication.java): entry point có `SpringBootApplication`.
- [AuditingConfig.java](product-service/src/main/java/org/example/micro/config/AuditingConfig.java): ví dụ tự khai báo bean `AuditorAware`.
- [SecurityConfig.java](api-gateway/src/main/java/org/example/apigateway/config/SecurityConfig.java): gateway security reactive dùng JWT.
- [pom.xml của product-service](product-service/pom.xml) và [pom.xml của order-service](order-service/pom.xml): dependency và version khai báo; không đại diện cho trạng thái runtime đã kiểm chứng.

### 23.2. ecomerc shop

- [ProductVariant.java](../ecomerc%20shop/ecommerc_shop_backend/src/main/java/org/example/ecommerc_shop/entity/ProductVariant.java): `@Version` và `quantityInStock`, dẫn chứng optimistic locking.
- [CouponRepository.java](../ecomerc%20shop/ecommerc_shop_backend/src/main/java/org/example/ecommerc_shop/repository/CouponRepository.java): pessimistic write lock khi chọn coupon hợp lệ.
- [CartItemRepository.java](../ecomerc%20shop/ecommerc_shop_backend/src/main/java/org/example/ecommerc_shop/repository/CartItemRepository.java): fetch join variant và product.
- [OrderRepository.java](../ecomerc%20shop/ecommerc_shop_backend/src/main/java/org/example/ecommerc_shop/repository/OrderRepository.java): EntityGraph user, query tổng hợp và Slice phục vụ export.
- [OrderTransactionService.java](../ecomerc%20shop/ecommerc_shop_backend/src/main/java/org/example/ecommerc_shop/service/impl/OrderTransactionService.java): nghiệp vụ đặt hàng, transaction, retry optimistic conflict và cập nhật coupon.
- [OrderServiceImpl.java](../ecomerc%20shop/ecommerc_shop_backend/src/main/java/org/example/ecommerc_shop/service/impl/OrderServiceImpl.java): gọi sang bean transaction riêng và tạo notification PENDING khi đổi một số trạng thái đơn.
- [CartServiceImpl.java](../ecomerc%20shop/ecommerc_shop_backend/src/main/java/org/example/ecommerc_shop/service/impl/CartServiceImpl.java): sử dụng fetch join để đọc giỏ hàng.
- [OrderItem.java](../ecomerc%20shop/ecommerc_shop_backend/src/main/java/org/example/ecommerc_shop/entity/OrderItem.java) và [OrderItemMapper.java](../ecomerc%20shop/ecommerc_shop_backend/src/main/java/org/example/ecommerc_shop/mapper/OrderItemMapper.java): snapshot `unitPrice` lấy từ variant lúc tạo dòng hàng.
- [NotificationRepository.java](../ecomerc%20shop/ecommerc_shop_backend/src/main/java/org/example/ecommerc_shop/repository/NotificationRepository.java): EntityGraph user khi đọc batch notification.
- [NotificationScheduler.java](../ecomerc%20shop/ecommerc_shop_backend/src/main/java/org/example/ecommerc_shop/scheduler/NotificationScheduler.java): gửi email trong transaction, ví dụ cần cân nhắc thu ngắn ranh giới transaction.
- [WebSecurityConfig.java](../ecomerc%20shop/ecommerc_shop_backend/src/main/java/org/example/ecommerc_shop/config/WebSecurityConfig.java): SecurityFilterChain, HTTP Basic, role và PasswordEncoder bean.
- [EcommercShopApplication.java](../ecomerc%20shop/ecommerc_shop_backend/src/main/java/org/example/ecommerc_shop/EcommercShopApplication.java): bật scheduling và retry.

### 23.3. Chủ đề chưa thấy triển khai trong source đã đọc

EAV, cột thuộc tính JSON, Closure Table, Materialized Path, full-text index, partition/shard routing, prototype lookup, custom aspect, `@Async`, `@Cacheable` và trigger: các phần liên quan trong tài liệu là lý thuyết hoặc phương án mở rộng. Với index và trigger, cần kiểm tra schema DB trực tiếp mới biết đầy đủ những cấu trúc có thể đã được tạo ngoài repo.

Các nguồn chính thức được đặt cạnh phần giải thích liên quan để bạn đọc sâu hơn. Chi tiết phụ thuộc engine, framework version hoặc cấu hình cần đối chiếu với môi trường thực tế khi triển khai.
