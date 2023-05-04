package com.kapps.calendaryt

class FullDate(val year: Int, val month : Int, val day : Int, val hours: List<String>): Comparable<FullDate>{
    override fun equals(other: Any?): Boolean {
        //return super.equals(other)
        val day = other as FullDate
        return day.day==this.day && day.month==this.month && day.year==this.year
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

    override fun toString(): String {
        return "$year/$month/$day"
    }

    override fun compareTo(other: FullDate): Int {
        return if(this.year>other.year){
             1
        }else if(this.year==other.year){
            if(this.month>other.month){
               1
            }else if(this.month==other.month){
                if(this.day>other.day){
                     1
                }else if(this.day==other.day){
                    0
                }else{
                    -1
                }
            }else{
                -1
            }
        }else{
            -1
        }
    }
}