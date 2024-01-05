package uz.prod.backcrm.payload;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class PaymentDTO {

    private UUID id;

    private PaymentTypeDTO paymentTypeDTO;

    private Double sum;

    private List<PaymentHistoryDTO> paymentHistoryDTOList;

}
