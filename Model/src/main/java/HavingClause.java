import java.io.Serializable;

public class HavingClause implements Serializable {
    private AggregateOperators aggregateOperators;
    private String operator;
    private String value;

    public HavingClause(){}

    public HavingClause(AggregateOperators aggregateOperators, String operator, String value) {
        this.aggregateOperators = aggregateOperators;
        this.operator = operator;
        this.value = value;
    }
    public HavingClause(String aggregateOperator, String operator, String value) {
        this.aggregateOperators = this.getHavingOperator(aggregateOperator);
        this.operator = operator;
        this.value = value;
    }

    public AggregateOperators getHavingOperator(String havingStringOperator){
        switch (havingStringOperator){
            case "COUNT":
                return AggregateOperators.COUNT;
            case "SUM":
                return AggregateOperators.SUM;
            case "AVG":
                return AggregateOperators.AVG;
        }
        return null;
    }

    public void setAggregateOperator(String aggregateStringOperator){
        if(aggregateStringOperator.contains("COUNT")){
            this.aggregateOperators = AggregateOperators.COUNT;
        }
        if(aggregateStringOperator.contains("SUM")){
            this.aggregateOperators = AggregateOperators.SUM;
        }
        if(aggregateStringOperator.contains("AVG")){
            this.aggregateOperators = AggregateOperators.AVG;
        }
    }

    public AggregateOperators getAggregateOperators() {
        return aggregateOperators;
    }

    public void setAggregateOperators(AggregateOperators aggregateOperators) {
        this.aggregateOperators = aggregateOperators;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
