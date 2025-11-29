package com.parkingmanage.query;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
public class OwnerQuery {
    private String province;

    private String city;

    private String district;
    private String community;
    private String building;

    private Integer units;

    private Integer floor;

    private Integer roomnumber;


}
